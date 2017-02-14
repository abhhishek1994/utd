COMPILE INSTRUCTION
    $> javac -d bin aos.*.java
This should have no warnings or errors.

EXECUTE INSTRUCTION
    $> ./aos.Server <port> <node ID> $HOME/launch/config.txt

This tells the program which node it is.

config.txt is formatted as:

<Size of network>

<node id 0> <host 0> <port 0>
<node id 0> <host 0> <port 0>
<node id 0> <host 0> <port 0>
<node id 0> <host 0> <port 0>
<node id 0> <host 0> <port 0>

<node id 0> <neighbor1 neighbor2 ...>
<node id 0> <neighbor1 neighbor2 ...>
<node id 0> <neighbor1 neighbor2 ...>
<node id 0> <neighbor1 neighbor2 ...>
<node id 0> <neighbor1 neighbor2 ...>
