#!/bin/bash

# Change this to your netid
netid=zxl165030

# Root directory of your project
PROJDIR=/home/011/z/zx/zxl165030/TestProj

# Directory where the config file is located on your local system
CONFIGLOCAL=$HOME/launch/config.txt

# Directory your java classes are in
BINDIR=$PROJDIR/bin

# Your main project class
PROG=aos.Server

n=0

# Set any line start with sign '#' empty.
# Delete the empty line.
cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
# Read a line from pipe line and assign it to a variable
    read i
    
# Pring number of line 
    echo $i
    while [ $n -lt $i ] 
    do
    	read line
    	n=$( echo $line | awk '{ print $1 }' )    # node id
        host=$( echo $line | awk '{ print $2 }' ) # host 
        port=$( echo $line | awk '{ print $3 }' ) # port

        echo "[Host = $netid@$host]"
        echo "[\$BINDIR = $BINDIR]"
        echo "[\$PROG = $PROG]"
        echo "[args = $port $n]"


	
         # `-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no` to bypass host key checking
	    bash -c "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host java -cp $BINDIR $PROG $port $n $CONFIGLOCAL; $SHELL" &

        n=$(( n + 1 ))
    done
)
