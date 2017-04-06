package edu.utdallas.project3.server;

/**
 * Tag enum for message 
 * @author zeqing
 *
 */
public enum Tag {
    DEFAULT,
    SETUP,
    MARKER,
    APP,
    HANDSHAKE,
    
    // Message to construct spanning tree. 
    // A Tree message must be either Invite, Accept, or Reject 
    TREE_INVITE,
    TREE_ACCEPT,
    TREE_REJECT,
    
    TREE_CONVERGE,
    TREE_BROADCAST;
}
