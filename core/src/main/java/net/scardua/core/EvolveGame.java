package net.scardua.core;

import net.scardua.ga.*;
import net.scardua.nn.NeuralNetwork;
import playn.core.*;

import java.util.ArrayList;

import static playn.core.PlayN.*;

public class EvolveGame extends Game.Default {

    private ArrayList<Robot> robots = null;
    private int numRobots = 20;
    private int numBalls = 10;
    private int numWeights = 0;
    private GeneticAlgorithm ga;
    private int ticks = 0;
    private int generation = 0;
    private final int ticksPerGeneration = 2000;
    private final int fastForwardSteps = 2000;
    private ArrayList<Ball> balls;

    private boolean fastForward = false;
    private Image ballImage;
    private Image botImage;

    public EvolveGame() {
        super(33); // call update every 33ms (30 times per second)
    }

    @Override
    public void init() {
        // create and add background image layer
        //Image bgImage = assets().getImage("images/bg.png");
        //ImageLayer bgLayer = graphics().createImageLayer(bgImage);
        //graphics().rootLayer().add(bgLayer);

        this.balls = new ArrayList<Ball>();

        this.ballImage = assets().getImage("images/ball.png");

        for(int i = 0; i < numBalls; i++) {
            Ball ball = new Ball();
            ImageLayer ballLayer = graphics().createImageLayer(ballImage);
            graphics().rootLayer().add(ballLayer);
            ball.addGraphics(ballLayer);
            this.balls.add(ball);
        }


        botImage = assets().getImage("images/robot.png");

        this.robots = new ArrayList<Robot>();
        for(int i = 0; i < numRobots; i++) {
            Robot robot = new Robot();
            ImageLayer botLayer = graphics().createImageLayer(botImage);
            graphics().rootLayer().add(botLayer);
            robot.addGraphics(botLayer);
            this.robots.add(robot);
        }

        Genes.getInstance()
                .addGene("red", FloatValue.class)
                .addGene("green", FloatValue.class)
                .addGene("blue", FloatValue.class)
        ;

        this.numWeights = this.robots.get(0).brain.getNumWeights();
        for(int i = 0; i < numWeights; i++) {
            String weightName = String.format("weight_%03d", i);
            Genes.getInstance().addGene(weightName, FloatValue.class);
        }

        this.ga = new GeneticAlgorithm(numRobots);
        for(int i = 0; i < numRobots; i++) {
            this.robots.get(i).loadChromosome(this.ga.getChromosomes().get(i));
        }

        pointer().setListener(new Pointer.Listener() {
            @Override
            public void onPointerStart(Pointer.Event event) {
            }

            @Override
            public void onPointerEnd(Pointer.Event event) {

                for(Ball ball : balls) {
                    if (Math.abs(ball.x - event.x()) < 16.0 && Math.abs(ball.y - event.y()) < 16.0) {
                        graphics().rootLayer().remove(ball.imageLayer);
                        balls.remove(ball);
                        numBalls--;
                        return;
                    }
                }
                Ball spawn = new Ball();
                spawn.x = event.x();
                spawn.y = event.y();
                ImageLayer ballLayer = graphics().createImageLayer(ballImage);
                graphics().rootLayer().add(ballLayer);
                spawn.addGraphics(ballLayer);
                balls.add(spawn);
                numBalls++;
            }

            @Override
            public void onPointerDrag(Pointer.Event event) {
            }

            @Override
            public void onPointerCancel(Pointer.Event event) {
            }
        });

        keyboard().setListener(new Keyboard.Adapter() {
            public void onKeyUp(Keyboard.Event event) {
                switch (event.key()) {
                    case MENU:
                    case F:
                        fastForward = !fastForward;
                        break;
                    case ESCAPE:
                        System.exit(0);
                }
            }
        });
    }

    @Override
    public void update(int delta) {
        if (fastForward) {
            for(int i = 0; i < fastForwardSteps; i++) {
                updateStep();
            }
        } else {
            updateStep();
        }
    }

    private void updateStep() {
        this.ticks++;
        if (this.ticks >= this.ticksPerGeneration) {
            this.ticks = 0;
            generationStep();
        }
        for(Ball ball : balls) {
            if (random() < 0.002) {
                ball.randomize();
                ball.updatePosition();
            }
        }
        for(Robot robot : this.robots) {
            int IN_LOOK_VECTOR_X = 0, IN_LOOK_VECTOR_Y = 1, IN_NEAREST_VECTOR_X = 2, IN_NEAREST_VECTOR_Y = 3;
            int IN_NEAREST_R = 4, IN_NEAREST_G = 5, IN_NEAREST_B = 6, IN_OWN_COLOR_R = 7, IN_OWN_COLOR_G = 8, IN_OWN_COLOR_B = 9;
            int OUT_LEFT_TRACK = 0, OUT_RIGHT_TRACK = 1;
            ArrayList<Double> input = new ArrayList<Double>();
            for(int i = 0; i < robot.numInputs; i++) {
                input.add(0.0);
            }
            input.set(IN_LOOK_VECTOR_X,  Math.cos(robot.angle));
            input.set(IN_LOOK_VECTOR_Y, -Math.sin(robot.angle));
            Ball nearest = getNearestBall(robot);
            if (nearest != null) {
                double dx = nearest.x - robot.x;
                double dy = nearest.y - robot.y;
                double d = Math.sqrt(dx*dx+dy*dy);
                dx /= d;
                dy /= d;
                input.set(IN_NEAREST_VECTOR_X, dx);
                input.set(IN_NEAREST_VECTOR_Y, dy);
                input.set(IN_NEAREST_R, nearest.color == Color.RED ? 1.0 : 0.0);
                input.set(IN_NEAREST_G, nearest.color == Color.GREEN ? 1.0 : 0.0);
                input.set(IN_NEAREST_B, nearest.color == Color.BLUE ? 1.0 : 0.0);
            } else {
                input.set(IN_NEAREST_VECTOR_X, 0.0);
                input.set(IN_NEAREST_VECTOR_Y, 0.0);
                input.set(IN_NEAREST_R, 0.0);
                input.set(IN_NEAREST_G, 0.0);
                input.set(IN_NEAREST_B, 0.0);
            }
            input.set(IN_OWN_COLOR_R, robot.color == Color.RED ? 1.0 : 0.0);
            input.set(IN_OWN_COLOR_G, robot.color == Color.GREEN ? 1.0 : 0.0);
            input.set(IN_OWN_COLOR_B, robot.color == Color.BLUE ? 1.0 : 0.0);
            ArrayList<Double> output = robot.brain.update(input);

            double leftTrack = output.get(OUT_LEFT_TRACK);
            double rightTrack = output.get(OUT_RIGHT_TRACK);

            robot.applyForces(leftTrack, rightTrack);
            robot.updatePosition();
            for(Ball ball : this.balls) {
                if (Math.abs(ball.x - robot.x) < 16.0 &&
                    Math.abs(ball.y - robot.y) < 16.0) {
                    if (ball.color != robot.color) {
                        robot.fitness += 100.0;
                    } else {
                        robot.fitness *= 0.75;
                    }
                    ball.randomize();
                    ball.updatePosition();
                }
            }
        }
        for(Ball ball : balls) {
            ball.applyForces();
            ball.updatePosition();
        }
    }

    private Ball getNearestBall(Robot robot) {
        Ball bestBall = null;
        double bestSquareDistance = Double.POSITIVE_INFINITY;
        for(Ball ball : this.balls) {
            // check if visible
            double dx = ball.x - robot.x;
            double dy = ball.y - robot.y;

            double ballAngle = Math.atan2(-dy, dx);
            double delta = ballAngle - robot.angle;

            if (Math.abs(delta) < 15 * Math.PI / 180
                || Math.abs(delta - 2 * Math.PI) < 15 * Math.PI / 180.0
                || Math.abs(delta + 2 * Math.PI) < 15 * Math.PI / 180.0
               ) {
                double squareDistance = dx*dx + dy*dy;
                if (squareDistance < bestSquareDistance) {
                    bestSquareDistance = squareDistance;
                    bestBall = ball;
                }
            }
        }
        return bestBall;
    }

    private void generationStep() {
        this.generation++;
        for(int i = 0; i < numRobots; i++) {
            this.ga.getChromosomes().get(i).setFitness( this.robots.get(i).fitness );
        }
        log().debug("Generation: " + this.generation);
        double best = Double.NEGATIVE_INFINITY;
        double average = 0;
        for(Chromosome chromosome: this.ga.getChromosomes()) {
            average += chromosome.getFitness();
            if (chromosome.getFitness() > best) {
                best = chromosome.getFitness();
            }
        }
        log().debug("Average fitness: " + average / numRobots + ", best fitness: " + best);
        this.ga.stepOver();
        for(int i = 0; i < numRobots; i++) {
            this.robots.get(i).loadChromosome(this.ga.getChromosomes().get(i));
        }
        for(Ball ball : this.balls) {
            ball.randomize();
            ball.updatePosition();
        }
    }

    @Override
    public void paint(float alpha) {
        // the background automatically paints itself, so no need to do anything here!
    }

    public static class Robot {
        public final int numInputs = 10;
        public final int numOutputs = 2;
        private final int numHiddenLayers = 3;
        private final int numNeuronsPerHiddenLayer = 6;
        private int numWeights = 0;

        public double x, y, angle;
        public double speed = 5.0;

        public double fitness = 0.0;

        public Color color;

        public NeuralNetwork brain;
        private ImageLayer imageLayer;
        private double maxTurnRate = 1.00;

        public Robot() {
            this.x = random() * graphics().width();
            this.y = random() * graphics().height();
            this.angle = random() * 2 * Math.PI;

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
            if (redness > greenness && redness > blueness) {
                this.color = Color.RED;
            } else if (greenness > blueness) {
                this.color = Color.GREEN;
            } else {
                this.color = Color.BLUE;
            }
            this.fitness = 0.0;
        }

        public void addGraphics(ImageLayer imageLayer) {
            this.imageLayer = imageLayer;
            this.imageLayer.setOrigin(16, 16);
            updatePosition();
        }

        public int getTint() {
            int tint = 0xff000000;
            if (this.color == Color.RED)   tint |= 0x00ff0000;
            if (this.color == Color.GREEN) tint |= 0x0000ff00;
            if (this.color == Color.BLUE)  tint |= 0x000000ff;
            return tint;
        }

        private void updatePosition() {
            this.imageLayer.setTranslation((float) this.x, (float) this.y);
            this.imageLayer.setRotation((float) -this.angle);
            this.imageLayer.setTint(this.getTint());
        }

        public void applyForces(double leftTrack, double rightTrack) {
            double rotForce = leftTrack - rightTrack;
            if (rotForce > this.maxTurnRate)  rotForce = this.maxTurnRate;
            if (rotForce < -this.maxTurnRate) rotForce = -this.maxTurnRate;

            this.angle += rotForce;

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
    }

    public class Ball {
        public double x;
        public double y;
        public double speed;
        public double angle;
        public Color color;
        private ImageLayer imageLayer;

        public Ball() {
            randomize();
        }

        public void randomize() {
            this.x = random() * graphics().width();
            this.y = random() * graphics().height();
            this.speed = 5.0 * random();
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

        private void updatePosition() {
            this.imageLayer.setTranslation((float) this.x, (float) this.y);
            this.imageLayer.setTint(this.getTint());
        }
    }

    private enum Color {
        RED, GREEN, BLUE
    }
}
