package edu.utdallas.project3.tools;
/**
 * Thrown when a required property is not present
 * 
 * 
 */
public class UndefinedPropertyException extends RuntimeException {

    private static final long serialVersionUID = 1;

    public UndefinedPropertyException(String variable) {
        super("Missing required property '" + variable + "'.");
    }

    public UndefinedPropertyException(String message, Exception cause) {
        super(message, cause);
    }

    public UndefinedPropertyException(Exception cause) {
        super(cause);
    }

}
