package edu.utdallas.project3.tools;


/**
 * Interface which all configuration filer loader class need to implement.
 * @author zeqing
 *
 */
public interface ConfigurationLoader {
    void loadConfig(String relativePath, int myId);
    void loadConfigFromAbs(String absolutePath, int myId);
}
