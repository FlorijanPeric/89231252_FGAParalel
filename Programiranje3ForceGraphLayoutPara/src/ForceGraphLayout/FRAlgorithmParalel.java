package ForceGraphLayout;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FRAlgorithmParalel {
    private Graph graph;
    private double area;
    private double koeficient;
    private double temperature;
    private int Iterations;
    private double constant;
    private double minMovementInt;
    private int width;
    private int height;
    private Random random;
    private boolean runProg;
    private double coolingRate;
    private double maxDispl=0;
    private ExecutorService exec;
    private boolean isBoolean;
    public FRAlgorithmParalel(Graph graph, int iterations, int width, int height, int seed, boolean useBool){
        this.random=new Random(seed);
        this.width=width;
        this.height=height;
        this.constant=seed;
        this.graph=graph;
        this.Iterations=iterations;
        this.area=width*height;
        this.koeficient=(Math.sqrt(area/graph.getNodes().size()));
        this.temperature= (double) Math.min(width, height) /100;
        this.coolingRate=0.97;
        //this.temperature=0.99;
        this.minMovementInt=0.1;
        this.runProg=false;
        this.exec= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.isBoolean=useBool;

    }
    public boolean run(){
            Logger.log("I calculating Repulsive F",LogLevel.Info);
            if(isBoolean){
                calculateParalelRepulsiveForces();
                calculateParalelAttractiveForces();
            }
            Logger.log("I am cooling",LogLevel.Debug);
            double maxUpdate=updatePosition();
            cooling();



            Logger.log("I am updating pos",LogLevel.Debug);

        return maxUpdate>=minMovementInt&&temperature>=0.0000000001;


    }
    /*private void calculateSequentialRepulsiveForces(){
        for(Node v:graph.getNodes()){
            v.dispX=0;
            v.dispY=0;
            for(Node u:graph.getNodes()){
                if(u!=v){
                    double dx=v.x-u.x;
                    double dy=v.y-u.y;
                    double distance=Math.sqrt(dx * dx + dy * dy);
                    if(distance==0) distance=0.01;
                    if(distance>0) {
                        v.dispX += (dx / distance) * forceRep(distance);
                        v.dispY += (dy / distance) * forceRep(distance);
                    }
                }
            }
        }
    }

     */
    private void calculateParalelRepulsiveForces(){
        //Prvo poštimamo kako bomo paralelizirali.
        List <Node> nodes=graph.getNodes();
        int howManyNodes=nodes.size();
        List <Runnable> task=new ArrayList<>();
        int chunk=howManyNodes/Runtime.getRuntime().availableProcessors();//Mormo
        //število nodov dt na procesorje de bo usak delau neki
        for(int i=0;i<howManyNodes;i+=chunk){
            int start=i;
            int end=Math.min(i+chunk,howManyNodes);//Boundaries
                task.add(()->{
                for (int j = start; j < end; j++) {
                    Node v = nodes.get(j);
                    v.dispY = 0;
                    v.dispX = 0;
                    for (Node u : nodes) {
                        if (u != v) {
                            double dx = v.x - u.x;
                            double dy = v.y - u.y;
                            double distance = Math.sqrt(dx * dx + dy * dy);
                            if (distance == 0) distance = 0.01;
                            if (distance > 0) {
                                v.dispX += (dx / distance) * forceRep(distance);
                                v.dispY += (dy / distance) * forceRep(distance);
                            }
                        }
                    }
                }
                });
            }
        for (Runnable v:task){
            exec.submit(v);
        }
        /*try {
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         */
    }
    /*private void calculateSequentialAttractiveForces(){
        for(Edge edge : graph.getEdgesUsed()){
            double dx=edge.start.x-edge.end.x;
            double dy=edge.start.y-edge.end.y;
            double distance=Math.sqrt(dx * dx + dy * dy);
                if(distance==0) distance=0.01;
            if(distance>0) {
                edge.start.dispX -= (dx / distance) * forceAttractive(distance);
                edge.start.dispY -= (dy / distance) * forceAttractive(distance);
                edge.end.dispX += (dx / distance) * forceAttractive(distance);
                edge.end.dispY += (dy / distance) * forceAttractive(distance);
            }
        }
    }

     */
    private void calculateParalelAttractiveForces(){
        List <Edge> edges=graph.getEdgesUsed();
        int howManyEdges=edges.size();
        int chunk=howManyEdges/Runtime.getRuntime().availableProcessors();
        List<Runnable> task=new ArrayList<>();
        for (int i=0;i<howManyEdges;i+=chunk){
            int start=i;
            int end=Math.min(i+chunk,howManyEdges);
            task.add(()->{
                for (int j = 0; j < end; j++) {
                    Edge edge=edges.get(j);
                        double dx=edge.start.x-edge.end.x;
                        double dy=edge.start.y-edge.end.y;
                        double distance=Math.sqrt(dx * dx + dy * dy);
                        if(distance==0) distance=0.01;
                        if(distance>0) {
                            edge.start.dispX -= (dx / distance) * forceAttractive(distance);
                            edge.start.dispY -= (dy / distance) * forceAttractive(distance);
                            edge.end.dispX += (dx / distance) * forceAttractive(distance);
                            edge.end.dispY += (dy / distance) * forceAttractive(distance);

                    }
                }
            });
        }
        for (Runnable v:task){
            exec.submit(v);
        }
        /*
        try {
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

         */
    }
    private double updatePosition(){

        for (Node v:graph.getNodes()){
            double display= Math.sqrt(v.dispX * v.dispX + v.dispY * v.dispY);
            maxDispl=Math.max(maxDispl,display);
            //if (display==0) display=0.01;
            if(display > 0) {
                v.x += (v.dispX / display) * Math.min(display, temperature);
                v.y += (v.dispY / display) * Math.min(display, temperature);
            }
            //v.x = Math.max(10,Math.min(v.x,width-10));
            //v.y = Math.min((double) height / 2, Math.max(((double) (height * -1) / 2), v.y));
            //v.y=Math.max(10,Math.min(v.x,width-10));
            v.x=Math.max(10,Math.min(v.x,width-10));
            v.y=Math.max(10,Math.min(v.y,height-10));
        }
        return maxDispl;
    }
    public void closeExec(){
        exec.shutdown();
    }
    private void cooling(){
        //temperature-=Math.max(temperature*0.95,0);
        temperature=Math.max(temperature*coolingRate,0);
        Logger.log("Temperature == "+temperature,LogLevel.Debug);
    }
    private double forceRep(double dist){
        return (koeficient * koeficient)/dist;
    }
    private double forceAttractive(double dist){
        return (dist * dist)/koeficient;
    }
}
