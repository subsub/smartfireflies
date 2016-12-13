package processing.test.fireflies;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;

public class fireflies extends PApplet {

    static Population population;
    static PVector target;
    static int targetRad = 30;
    int finishedFirefly = 0;

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[]{"fireflies"};
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }

    public void setup() {

        orientation(PORTRAIT);
        population = new Population();
        target = new PVector(width / 2, 200);
    }

    public void draw() {
        background(0);
        population.fly();
        showTarget();
    }

    public void showTarget() {
        if (finishedFirefly >= population.popsize * 2) {
            finishedFirefly = 0;
            target = new PVector(random(width), random(100, 1500));
            targetRad = 30;
        }
        pushMatrix();
        fill(255, 150);
        noStroke();
        ellipse(target.x, target.y, targetRad, targetRad);
        popMatrix();
    }

    public void settings() {
        fullScreen();
    }

    class DNA {
        int len = 1000;
        PVector[] genes;

        public DNA() {
            genes = new PVector[len];
            for (int i = 0; i < len; i++) {
                genes[i] = PVector.random2D();
                genes[i].setMag(0.1f);
            }
        }

        public DNA(PVector[] genes) {
            this.genes = genes;
        }

        public DNA crossOver(DNA partner) {
            PVector[] newgenes = new PVector[len];
            int midpoint = floor(random(genes.length));
            for (int i = 0; i < genes.length; i++) {
                if (i > midpoint) {
                    newgenes[i] = genes[i];
                } else {
                    newgenes[i] = partner.genes[i];
                }
            }
            return new DNA(newgenes);
        }

        public void mutation() {
            for (int i = 0; i < genes.length; i++) {
                if (random(i) < 0.01) {
                    this.genes[i] = PVector.random2D();
                    this.genes[i].setMag(0.1f);
                }
            }
        }
    }

    class FireFly {
        public static final int lifespan = 1000;
        PVector pos;
        PVector vel;
        PVector acc;
        DNA dna;
        float alpha;
        float fitness;
        int duration = 0;
        boolean finished = false;
        boolean dead = false;
        int count = 0;
        int colorR = 0;
        int colorG = 0;
        int colorB = 0;

        public FireFly() {
            this.dna = new DNA();
            init();
        }

        public FireFly(DNA dna) {
            this.dna = dna;
            init();
        }

        public void init() {
            pos = new PVector(width / 2, height);
            vel = new PVector();
            acc = new PVector();
            colorR = floor(random(0, 255));
            colorG = floor(random(0, 255));
            colorB = floor(random(0, 255));
        }

        public void applyForce(PVector force) {
            this.acc.add(force);
        }

        public void update(float alpha) {
            applyForce(this.dna.genes[count]);
            this.vel.add(this.acc);
            this.vel.limit(4);
            this.acc.mult(0);
            this.alpha = alpha;

            float d = dist(this.pos.x, this.pos.y, target.x, target.y);
            this.finished = d <= targetRad / 2;

            this.dead = this.pos.x < 0 || this.pos.x > width || this.pos.y > height || this.pos.y < 0;

            if (finished) {
                this.vel = new PVector();
                this.pos = target;
                targetRad += 2;
                finishedFirefly++;
            } else if (dead) {

            } else {
                this.pos.add(this.vel);
            }

            if (!dead && !finished) {
                duration++;
            }

            count++;
            if (count == lifespan) {
                count = 0;
            }
        }

        public void fly() {
            pushMatrix();
            translate(this.pos.x, this.pos.y);
            rotate(this.vel.heading());
            if (dead) {
                fill(255, 0, 0, 100);
            } else {
                fill(210, 210, 0, alpha);
            }
            noStroke();
            ellipseMode(CENTER);
            ellipse(0, 0, 10, 7);
            popMatrix();
        }

        public void calculateFitness() {
            float d = dist(this.pos.x, this.pos.y, target.x, target.y);
            this.fitness = map(d, 0, height, height, 0);
            if (finished) {
                this.fitness *= 10;
                this.fitness *= (1000 * this.fitness / this.duration);
            }
            if (dead) {
                this.fitness = 1;
            }
        }
    }

    class Population {
        FireFly[] fireflies;
        ArrayList<FireFly> matingPool = new ArrayList<FireFly>();
        int popsize = 100;

        public Population() {
            fireflies = new FireFly[popsize];
            for (int i = 0; i < popsize; i++) {
                this.fireflies[i] = new FireFly();
            }
        }

        public void fly() {
            for (int i = 0; i < popsize; i++) {
                if (this.fireflies[i].dead || this.fireflies[i].finished) {
                    evaluate();
                    this.fireflies[i] = selection();
                }
                this.fireflies[i].update(random(50, 255));
                this.fireflies[i].fly();
            }
        }

        public void evaluate() {
            float maxfit = 0;
            for (int i = 0; i < popsize; i++) {
                this.fireflies[i].calculateFitness();
                if (this.fireflies[i].fitness > maxfit) {
                    maxfit = this.fireflies[i].fitness;
                }
            }
            println(maxfit);

            for (int i = 0; i < popsize; i++) {
                this.fireflies[i].fitness /= maxfit;
            }

            matingPool.clear();
            for (int i = 0; i < popsize; i++) {
                float n = this.fireflies[i].fitness * 100;
                for (int j = 0; j < n; j++) {
                    matingPool.add(this.fireflies[i]);
                }
            }
        }

        public void selections() {
            FireFly[] newFireflies = new FireFly[popsize];
            for (int i = 0; i < this.fireflies.length; i++) {
                newFireflies[i] = selection();
            }
            this.fireflies = newFireflies;
        }

        public FireFly selection() {
            int indexA = PApplet.parseInt(random(matingPool.size()));
            int indexB = PApplet.parseInt(random(matingPool.size()));
            DNA parentDNAA = matingPool.get(indexA).dna;
            DNA parentDNAB = matingPool.get(indexB).dna;
            DNA child = parentDNAA.crossOver(parentDNAB);
            child.mutation();
            FireFly f = new FireFly(child);

            FireFly parentA = new FireFly(parentDNAA);
            FireFly parentB = new FireFly(parentDNAB);

            int colorRrange = floor((parentA.colorR + parentB.colorR) / 2);
            int colorGrange = floor((parentA.colorG + parentB.colorG) / 2);
            int colorBrange = floor((parentA.colorB + parentB.colorB) / 2);
            f.colorG = floor(random(colorRrange));
            f.colorG = floor(random(colorGrange));
            f.colorB = floor(random(colorBrange));
            return f;
        }
    }
}
