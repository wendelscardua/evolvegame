package net.scardua.java;

import playn.core.PlayN;
import playn.java.JavaPlatform;

import net.scardua.core.EvolveGame;

public class EvolveGameJava {

  public static void main(String[] args) {
    JavaPlatform.Config config = new JavaPlatform.Config();
    // use config to customize the Java platform, if needed
    config.width = 1024;
    config.height = 600;
    JavaPlatform.register(config);
    PlayN.run(new EvolveGame());
  }
}
