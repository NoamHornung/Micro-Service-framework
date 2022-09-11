package bgu.spl.mics.application.objects;

/**
 * Passive object representing a data used by a model.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */

public class DataBatch {
    private Data data; //the data the batch belongs to
    private int start_index;

    DataBatch(Data data, int start_index){
        this.data = data;
        this.start_index = start_index;
    }

    Data.Type getDataType(){
        return data.getType();
    }

    public Data getData() {
        return data;
    }
}