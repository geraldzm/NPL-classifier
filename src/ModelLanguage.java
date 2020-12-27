import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ModelLanguage extends Thread implements Comparator<ModelLanguage> {

    private final File subFolder;
    private final MyHashTable model;
    private final ModelFile mysteryVector;

    private final int nGram;
    private double vectorDistance; // the distance with this vector and the mysteryVector


    public ModelLanguage(File subFolder, int nGram, ModelFile mysteryVector) {
        this.subFolder = subFolder;
        this.nGram = nGram;
        this.mysteryVector = mysteryVector;
        vectorDistance = -5;
        model = new MyHashTable();
    }

    @Override
    public void run() {

        List<ModelFile> txts = Arrays.stream(
                Objects.requireNonNull(subFolder.list((file, s) -> s.matches("^.*\\.txt$")))// get each *.txt file
        ).map(str -> new ModelFile(new File(subFolder, str), model, nGram)).collect(Collectors.toList());

        vectorDistance = Math.acos(-1);

        if(txts.size() == 0)return;// if there is no txts

        new ThreadPool().executeAndAwait(txts); // executes all txt threads and waits until they finish

        try {
            mysteryVector.join(); // wait until the model of the mysteryFile is ready
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        double mysteryNorm = mysteryVector.getNorm();
        double languageNorm = txts.get(0).getNorm();

        AtomicInteger dotProduct = new AtomicInteger();

        mysteryVector.getVector()
                .forEach((str, frequency) -> {
                    Integer k = model.get(str);
                    if(k != null)
                        dotProduct.getAndAdd(k*frequency);
                });

        vectorDistance = dotProduct.get()/(mysteryNorm*languageNorm);

        // round 4 digits precision to avoid decimal trash i.e 1.0000000000000002
        vectorDistance = (double)Math.round(vectorDistance * 10000d) / 10000d;

        // cosine similarity in radians
        vectorDistance = Math.acos(vectorDistance);
        System.out.println(String.format("%s angle: %f", getNameFolder(), Math.toDegrees(vectorDistance)));
    }

    public String getNameFolder(){
        return subFolder.getName();
    }

    public double getVectorDistance(){
        return vectorDistance;
    }

    @Override
    public int compare(ModelLanguage v1, ModelLanguage v2) {
        if(v1.vectorDistance == v2.vectorDistance) return 0;
        return v1.vectorDistance < v2.vectorDistance ? -1: 1;
    }
}
