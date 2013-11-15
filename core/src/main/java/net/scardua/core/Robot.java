package net.scardua.core;

import net.scardua.ga.Chromosome;
import net.scardua.ga.Value;
import net.scardua.nn.NeuralNetwork;
import playn.core.ImageLayer;

import java.util.ArrayList;

import static playn.core.PlayN.graphics;
import static playn.core.PlayN.random;

/**
* Created with IntelliJ IDEA.
* User: wendel
* Date: 11/12/13
* Time: 21:01
*/
public class Robot implements Position {
    public final int numInputs = 16;
    public final int numOutputs = 5;
    private final int numHiddenLayers = 3;
    private final int numNeuronsPerHiddenLayer = 8;
    private int numWeights = 0;

    public double x, y, angle;
    public double speed = 3.0;

    public double fitness = 0.0;
    public final int maxPower = 500;
    public int power = maxPower;
    public int deltaPower = 0;
    public int timeOfDeath = -1;
    public Color color;

    public NeuralNetwork brain;
    private ImageLayer imageLayer;
    private double maxTurnRate = .3;
    private double fov;

    public Robot() {
        this.brain = new NeuralNetwork(numInputs, numOutputs, numHiddenLayers, numNeuronsPerHiddenLayer);
        this.numWeights = brain.getNumWeights();
    }

    public void loadChromosome(Chromosome chromosome) {
        ArrayList<Double> weights = new ArrayList<Double>();
        for(int i = 0; i < this.numWeights; i++) {
            Value value = chromosome.getGeneValue(String.format("weight_%03d", i));
            weights.add(value.getFloatValue());
        }
        this.brain.putWeights(weights);

        double redness = chromosome.getGeneValue("red").getFloatValue();
        double greenness = chromosome.getGeneValue("green").getFloatValue();
        double blueness = chromosome.getGeneValue("blue").getFloatValue();
        double fov = chromosome.getGeneValue("fov").getFloatValue();
        this.fov = fov;
        if (redness > greenness && redness > blueness) {
            this.color = Color.RED;
        } else if (greenness > blueness) {
            this.color = Color.GREEN;
        } else {
            this.color = Color.BLUE;
        }
        this.fitness = 0.0;
        this.power = this.maxPower;
        randomize();
    }

    public void addGraphics(ImageLayer imageLayer) {
        this.imageLayer = imageLayer;
        this.imageLayer.setOrigin(16, 16);
        updatePosition();
    }

    public int getTint() {
        int tint = 0xff000000;
        int value = (int) (0x30 + (0xff - 0x30) * this.power / (float) this.maxPower);
        if (this.color == Color.RED)   tint |= (value << 16);
        if (this.color == Color.GREEN) tint |= (value << 8);
        if (this.color == Color.BLUE)  tint |= value;
        return tint;
    }

    public double getFOV() {
        return this.fov;
    }

    public void randomize() {
        this.x = random() * graphics().width();
        this.y = random() * graphics().height();
        this.angle = random() * 2 * Math.PI;
    }

    public void updatePosition() {
        this.imageLayer.setTranslation((float) this.x, (float) this.y);
        this.imageLayer.setRotation((float) -this.angle);
        this.imageLayer.setTint(this.getTint());
    }

    public void applyForces(double leftTrack, double rightTrack, double speed) {
        double rotForce = leftTrack - rightTrack;
        if (rotForce >= this.maxTurnRate) { rotForce = this.maxTurnRate; }
        else if (rotForce <= -this.maxTurnRate) { rotForce = -this.maxTurnRate; }
        this.deltaPower -= 1;


        this.angle += rotForce;

        double applySpeed = this.speed * 2 * (speed - 0.5); // speed = 1 => 1 (forward). speed = 0 => -1 (rewind)

        double x2 = this.x +  Math.cos(this.angle) * (applySpeed);
        double y2 = this.y + -Math.sin(this.angle) * (applySpeed);

        if (x2 < 0) { x2 = 0.0; this.angle = Math.PI - this.angle; }
        if (x2 > graphics().width()) { x2 = graphics().width(); this.angle = Math.PI - this.angle; }
        if (y2 < 0) { y2 = 0.0; this.angle = 2 * Math.PI - this.angle; this.deltaPower -= 10; }
        if (y2 > graphics().height()) { y2 = graphics().height(); this.angle = 2 * Math.PI - this.angle; }

        this.x = x2;
        this.y = y2;
        while (this.angle < 0) this.angle += 2 * Math.PI;
        while (this.angle >= 2 * Math.PI) this.angle -= 2 * Math.PI;
    }

    @Override
    public float x() {
        return (float) x;
    }

    @Override
    public float y() {
        return (float) y;
    }
}
