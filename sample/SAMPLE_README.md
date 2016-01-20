#1. run.sh
The file <b>run.sh</b> is the entrance of starting the process.
<b>/usr/local/spark-1.3.1-bin-hadoop2.6/bin/spark-submit</b> : absolute path of the spark-submit shell.
<b>TASConNASC/MultiClassSVM-assembly-1.0.3.jar</b> : relative path of the assembled spark jar.
<b>TASConNASC/SpecialDomain/TeamFollowBack/config.xml</b> : relative path of the configuration file , which contains relative paths of the input and output files.

#2. config.xml
All the input files and output dictionary for the classify process are already showed in ./TASConNASC in relative paths.
You can use absolute path instead of relative path.

#3. spark parameter
Find  /spark-1.3.1-bin-hadoop2.6/conf/spark-env.sh (first time colone a copy from spark-env.sh)
and Enable the following two parameters:
export SPARK_EXECUTOR_MEMORY=4G
export SPARK_DRIVER_MEMORY=16G
.
