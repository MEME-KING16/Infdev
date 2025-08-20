package net.infdev.api;

public interface Mod {
    /**
     * Run stuff on init
    */
    void onInitialize();

    /**
     * When game rdy to reg items
    */
    void OnRegister();

    /**
     * When game start
    */
    default void onStart() {};

    /**
     * When game unstart
    */
    default void onShutdown() {};

    /**
     * @param key (String) The name pf the key you want something to happen when pressed
     */
    default void onKeyPressed(String key) {};

    /**
     * @param key (String) The name pf the key you want something to happen when pressed
     */
    //default boolean isKeyPressed(String key) {};
}