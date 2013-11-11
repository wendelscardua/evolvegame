package net.scardua.android;

import playn.android.GameActivity;
import playn.core.PlayN;

import net.scardua.core.EvolveGame;

public class EvolveGameActivity extends GameActivity {

  @Override
  public void main(){
    PlayN.run(new EvolveGame());
  }
}
