#!/usr/bin/env bash

#
#  Deploys the gemfire-changes-subscriber kafka listener to the bridge servers
#

echoerr() { echo "$@" 1>&2; }
badopt() { echoerr "$@"; echo ""; HELP='true'; }

DEPLOYMENT_LOCATION=""
APP_NAME="gemfire-changes-subscriber"
LOG_LOCATION="/var/log/kafka-consumers"

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      HELP='true'; break ;;
    -a)
      ADD_FLAG='true'; shift ;;
    -s)
      SECURITY_FLAG='true'; shift ;;
    -n)
      shift
      if test $# -gt 0; then
        INSTANCE_COUNT=$1
      else
        badopt "Please specify an instance count with the -n option"
        break
      fi
      shift ;;
    -b)
      shift
      if test $# -gt 0; then
        BRIDGE_SERVERS=$1
      else
        badopt "Please provide a server list with the -b option"
        break
      fi
      shift ;;
    -p)
      shift
      if test $# -gt 0; then
        SPRING_PROFILE_ENV=$1
      else
        badopt "Please provide a server list with the -p option"
        break
      fi
      shift ;;
    *)
      if [[ -z $DESTINATION ]]; then
        DESTINATION=$1
        shift
      else
        # Check first character of parameter for unexpected options
        if [[ "${1:0:1}" == "-" ]]; then
          badopt "Unknown option $1 specified."
          break
        else
          break
        fi
      fi
      ;;
  esac
done

if [[ -z $SPRING_PROFILE_ENV || -z $DESTINATION || $HELP ]]; then
  echoerr "Usage: $0 [OPTIONS] destination -p spring-profile"
  echoerr '  -p "profile"                Spring profile to run the subscriber with'
  echoerr 'Optional Flags:'
  echoerr '  -a                          Add new instances without removing old ones'
  echoerr '  -n [int]                    Number of instances to run per server'
  echoerr '  -b "host1:port host2:port"  List of bridge servers to deploy listener to'
  echoerr '  -s                          Enable gemfire security configuration'
  echoerr '  -h, --help                  Display this help message'
  echoerr ''
  echoerr 'Examples:'
  echoerr "    $0 sso_rememberMeTicket-dev-data -p dev-data-to-sso"
  echoerr "    $0 sso_rememberMeTicket-dev-data -p dev-data-to-sso -a -n 2 -b 'olaxta-itwgfbridge00 olaxta-itwgfbridge01'"
  exit 1
fi

# Include gfsecurity config for subscribers that need to connect to secured clusters
TARGET_CLUSTER=`echo "$SPRING_PROFILE_ENV" | cut -d'-' -f3-`
if [[ $TARGET_CLUSTER == "to-sso" || $TARGET_CLUSTER == "to-customer" ]]; then
    [[ -z $SECURITY_FLAG ]] && SECURITY_FLAG='true'
fi

# Set environment specific configuration
[[ -z $INSTANCE_COUNT ]] && INSTANCE_COUNT=1
DEPLOYMENT_ENV=`echo "$SPRING_PROFILE_ENV" | cut -d'-' -f1`
if [[ "$DEPLOYMENT_ENV" == "poc" || "$DEPLOYMENT_ENV" == "dev" ]]; then
  [[ -z $KAFKA_JAAS_CONFIG ]]           && KAFKA_JAAS_CONFIG="/etc/config/kafka_client_jaas_plain.conf"
  [[ -z $SECURITY_FLAG ]]               && GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-dev.properties"   || GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-dev-no-ssl.properties"
  [[ -z $BRIDGE_SERVERS ]]              && BRIDGE_SERVERS="olaxta-itwgfbridge00"
elif [[ "$DEPLOYMENT_ENV" == "test" ]]; then
  [[ -z $KAFKA_JAAS_CONFIG ]]           && KAFKA_JAAS_CONFIG="/web/secure-config/gemfire/kafka_client_jaas_plain.conf"
  [[ -z $SECURITY_FLAG ]]               && GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-test.properties"  || GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-test-no-ssl.properties"
  [[ -z $BRIDGE_SERVERS ]]              && BRIDGE_SERVERS="olaxta-itwgfbridge00"
elif [[ "$DEPLOYMENT_ENV" == "stage" ]]; then
  [[ -z $KAFKA_JAAS_CONFIG ]]           && KAFKA_JAAS_CONFIG="/web/secure-config/gemfire/kafka_client_jaas_plain.conf"
  [[ -z $SECURITY_FLAG ]]               && GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-stage.properties" || GEMFIRE_SECURITY_PROPERTIES="/web/secure-config/gemfire/gfsecurity-stage-no-ssl.properties"
  [[ -z $BRIDGE_SERVERS ]]              && BRIDGE_SERVERS="olaxsa-itwgfbridge00"
elif [[ "$DEPLOYMENT_ENV" == "prod" ]]; then
  echoerr "TODO: Fill in prod details"; exit 1;
else
  echoerr "Unknown environment '$SPRING_PROFILE_ENV' provided. Exiting..."
  exit 1
fi

# BUILD
if [[ ! -e ~/.m2/settings.xml ]]; then
  echoerr "WARNING: No settings.xml found in ~/.m2/, build could fail when locating dependencies. Enter to continue, Ctrl-C to quit."
  read
fi

echo -n "Build artifact? Type y for yes: "
read INPUT
if [[ "$INPUT" == "y" ]]; then
  git pull
  mvn install
  if [[ $? -ne 0 ]]; then
    echoerr "ERROR: Build failed. Exiting deploy script."
    exit 1
  fi
fi
JAR_NAME=`ls -t target/$APP_NAME*.jar | cut -d'/' -f2 | head -n1`

# DEPLOY
START_COMMAND="java -Djava.security.auth.login.config=$KAFKA_JAAS_CONFIG"
START_COMMAND="$START_COMMAND -DgemfireSecurityPropertyFile=$GEMFIRE_SECURITY_PROPERTIES"
START_COMMAND="$START_COMMAND -Dspring.config.location=/web/config/"
START_COMMAND="$START_COMMAND -Dspring.profiles.active=$SPRING_PROFILE_ENV -jar $JAR_NAME"
START_COMMAND="$START_COMMAND --spring.cloud.stream.bindings.input.destination=$DESTINATION"
START_COMMAND="$START_COMMAND --logging.file=$LOG_LOCATION/$DESTINATION.log"
for host in $BRIDGE_SERVERS; do
  # Copy jar to the servers
  if [[ `ssh-copy-id -n $host 2>&1 | grep -c "All keys were skipped"` -lt 1 ]]; then
    if [[ `ssh-copy-id $host 2>&1 | grep "ERROR: No identities found"` ]]; then
      echoerr "No keys found for ssh-copy-id."
    else
      ssh-copy-id $host
    fi
  fi
  scp -q "target/$JAR_NAME" $host:$DEPLOYMENT_LOCATION

  # Remove previous instances of application
  if [[ -z ADD_FLAG || "$ADD_FLAG" != "true" ]]; then
    PIDS_TO_DELETE=`ssh -q $host "ps -ef | grep java | grep -v grep | grep $DESTINATION" |  tr -s ' ' | cut -d' ' -f2`
    if [[ $PIDS_TO_DELETE ]]; then
      echo "Removing `echo $PIDS_TO_DELETE | wc -w` existing processes from $host"
      ssh -q $host "kill `echo $PIDS_TO_DELETE` || sudo kill `echo $PIDS_TO_DELETE`"
    fi
  fi

  for (( i = 1; i <= $INSTANCE_COUNT; i++ )); do
    # Find an open port and start the listener
    PORT=8081
    PORTS_TAKEN=`ssh -q $host "ps -ef | grep java | egrep -o '\-\-server\.port=[0-9]+' | egrep -o '[0-9]+'"`
    while [[ `echo "$PORTS_TAKEN" | grep $PORT` ]]; do
      PORT=$((PORT+1))
    done
    echo "Starting app $DESTINATION on $host at port $PORT..."
    echo "nohup $START_COMMAND --server.port=$PORT >/dev/null 2>/dev/null </dev/null &"
    ssh -q $host "nohup $START_COMMAND --server.port=$PORT >/dev/null 2>/dev/null </dev/null &"  # input and output redirects with /dev/null needed for ssh/nohup combo
    ssh -q $host "chown :chgadm $LOG_LOCATION/* --silent"
  done
done
