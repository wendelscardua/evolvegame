package net.scardua.core;

import playn.core.ImageLayer;

import static playn.core.PlayN.graphics;
import static playn.core.PlayN.random;

/**
* Created with IntelliJ IDEA.
* User: wendel
* Date: 11/12/13
* Time: 21:01
*/
public class Ball implements Position {
    public double x;
    public double y;
    public double speed;
    public double angle;
    public Color color;
    public ImageLayer imageLayer;

    public Ball() {
        randomize();
    }

    public void randomize() {
//        double dx = (random()+.4) * .05 * graphics().width() * (random() > .5 ? 1 : -1);
//        double dy = (random()+.4) * .05 * graphics().height() * (random() > .5 ? 1 : -1);
//        if (this.x + dx <= 0 || this.x + dx > graphics().width()) dx = -dx;
//        if (this.y + dy <= 0 || this.y + dy > graphics().height()) dy = -dy;
//        this.x += dx;
//        this.y += dy;
        this.x = random() * graphics().width();
        this.y = random() * graphics().height();
        this.speed = random();
        this.angle = random() * 2 * Math.PI;
        double colorFactor = random();
        if (colorFactor < 1/3.0) {
            color = Color.RED;
        } else if (colorFactor < 2/3.0) {
            color = Color.GREEN;
        } else {
            color = Color.BLUE;
        }
    }

    public int getTint() {
        int tint = 0xff000000;
        if (this.color == Color.RED)   tint |= 0x00ff0000;
        if (this.color == Color.GREEN) tint |= 0x0000ff00;
        if (this.color == Color.BLUE)  tint |= 0x000000ff;
        return tint;
    }

    public void addGraphics(ImageLayer imageLayer) {
        this.imageLayer = imageLayer;
        this.imageLayer.setOrigin(16, 16);
        updatePosition();
    }

    public void applyForces() {
        double x2 = this.x +  Math.cos(this.angle) * (this.speed);
        double y2 = this.y + -Math.sin(this.angle) * (this.speed);

        if (x2 < 0) { x2 = 0.0; this.angle = Math.PI - this.angle; }
        if (x2 > graphics().width()) { x2 = graphics().width(); this.angle = Math.PI - this.angle; }
        if (y2 < 0) { y2 = 0.0; this.angle = 2 * Math.PI - this.angle; }
        if (y2 > graphics().height()) { y2 = graphics().height(); this.angle = 2 * Math.PI - this.angle; }

        this.x = x2;
        this.y = y2;
        while (this.angle < 0) this.angle += 2 * Math.PI;
        while (this.angle >= 2 * Math.PI) this.angle -= 2 * Math.PI;
    }

    public void updatePosition() {
        this.imageLayer.setTranslation((float) this.x, (float) this.y);
        this.imageLayer.setTint(this.getTint());
    }

    @Override
    public float x() {
        return (float) x;
    }

    @Override
    public float y() {
        return (float) y;
    }

    public void flip() {
        switch (this.color) {
            case RED:
                this.color = Color.GREEN;
                break;
            case GREEN:
                this.color = Color.BLUE;
                break;
            case BLUE:
                this.color = Color.RED;
                break;
        }
    }
}
