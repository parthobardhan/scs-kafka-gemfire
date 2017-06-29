#!/usr/bin/env bash

#
#  Deploys the gemfire-changes-subscriber kafka listener to the bridge servers
#

echoerr() { echo "$@" 1>&2; }
badopt() { echoerr "$@"; echo ""; HELP='true'; }

DEPLOYMENT_LOCATION=""
APP_NAME="gemfire-changes-subscriber"
LOG_LOCATION="."

while test $# -gt 0; do
  case "$1" in
    -h|--help)
      HELP='true'; break ;;
    -a)
      ADD_FLAG='true'; shift ;;
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
    *)
      if [[ -z $ENV ]]; then
        ENV=$1
        shift
      elif [[ -z $DESTINATION ]]; then
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

if [[ -z $ENV || -z $DESTINATION || $HELP ]]; then
  echoerr "Usage: $0 [OPTIONS] (poc|dev|test|stage|prod) destination"
  echoerr 'Optional Flags:'
  echoerr '  -a                          Add new instances without removing old ones'
  echoerr '  -n [int]                    Number of instances to run per server'
  echoerr '  -b "host1:port host2:port"  List of bridge servers to deploy listener to'
  echoerr '  -h, --help                  Display this help message'
  echoerr ''
  echoerr 'Examples:'
  echoerr "    $0 dev sso_rememberMeTicket-dev-data"
  echoerr "    $0 dev sso_rememberMeTicket-dev-data -a -n 2 -b 'olaxta-itwgfbridge00 olaxta-itwgfbridge01'"
  exit 1
fi

[[ -z INSTANCE_COUNT ]] && INSTANCE_COUNT=1
if [[ "$ENV" == "poc" || "$ENV" == "dev" || "$ENV" == "test" || "$ENV" == "stage" ]]; then
  [[ -z $BRIDGE_SERVERS ]]          && BRIDGE_SERVERS="olaxta-itwgfbridge00 olaxta-itwgfbridge01"
  [[ -z $KAFKA_BOOTSTRAP_SERVERS ]] && KAFKA_BOOTSTRAP_SERVERS="olaxda-itwgfkafka07.garmin.com:9092,olaxda-itwgfkafka08.garmin.com:9092,olaxda-itwgfkafka09.garmin.com:9092"
  [[ -z $ZOOKEEPER_NODES ]]         && ZOOKEEPER_NODES="olaxda-itwgfkafka07.garmin.com:2181,olaxda-itwgfkafka08.garmin.com:2181,olaxda-itwgfkafka09.garmin.com:2181"
elif [[ "$ENV" == "prod" ]]; then
  echoerr "TODO: Fill in prod details"; exit 1;
else
  echoerr "Unknown environment '$ENV' provided. Exiting..."
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

# DEPLOY
START_COMMAND="java -Djava.security.auth.login.config=/etc/config/kafka_client_jaas_plain.conf -jar $APP_NAME*.jar"
START_COMMAND="$START_COMMAND --spring.kafka.bootstrap-servers=$KAFKA_BOOTSTRAP_SERVERS"
START_COMMAND="$START_COMMAND --spring.cloud.stream.kafka.binder.zkNodes=$ZOOKEEPER_NODES"
START_COMMAND="$START_COMMAND --spring.cloud.stream.bindings.input.destination=$DESTINATION"
for host in $BRIDGE_SERVERS; do
  # Copy jar to the servers
  if [[ `ssh-copy-id -n $host 2>&1 | grep -c "All keys were skipped"` -lt 1 ]]; then
    if [[ `ssh-copy-id $host 2>&1 | grep "ERROR: No identities found"` ]]; then
      echoerr "No keys found for ssh-copy-id."
    else
      ssh-copy-id $host
    fi
  fi
  scp -q target/$APP_NAME*.jar $host:$DEPLOYMENT_LOCATION
  
  # Remove previous instances of application
  if [[ -z ADD_FLAG || "$ADD_FLAG" != "true" ]]; then
    PIDS_TO_DELETE=`ssh -q $host "ps -ef | grep java | grep -v grep | grep $DESTINATION" |  tr -s ' ' | cut -d' ' -f2`
    if [[ $PIDS_TO_DELETE ]]; then
      echo "Removing `echo $PIDS_TO_DELETE | wc -w` existing processes from $host"
      ssh -q $host "kill `echo $PIDS_TO_DELETE`"
    fi
  fi
  
  for (( i=1; i<=$INSTANCE_COUNT; i++ )); do
    # Find an open port and start the listener
    PORT=8081
    PORTS_TAKEN=`ssh -q $host "ps -ef | grep java | egrep -o '\-\-server\.port=[0-9]+' | egrep -o '[0-9]+'"`
    while [[ `echo "$PORTS_TAKEN" | grep $PORT` ]]; do
      PORT=$((PORT+1))
    done
    echo "Starting app $DESTINATION on $host at port $PORT..."
    ssh -q $host "nohup $START_COMMAND --server.port=$PORT > $LOG_LOCATION/$DESTINATION-$i.out 2>$LOG_LOCATION/$DESTINATION-$i.err < /dev/null &"
  done
done
