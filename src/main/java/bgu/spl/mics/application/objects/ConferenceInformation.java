package bgu.spl.mics.application.objects;

import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * Passive object representing information on a conference.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class ConferenceInformation {

    private String name;
    private int date;
    private ArrayList<Model> models;

    public ConferenceInformation(String name, int date){
        this.name= name;
        this.date= date;
        models= new ArrayList<>();
    }

    public void addModel(Model model){ //synchronized?????????
        models.add(model);
    }

    public ArrayList<Model> getModels() {
        return models;
    }

    public String getName() {
        return name;
    }

    public int getDate() {
        return date;
    }

    public JSONObject toJson(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("date",date);
        ArrayList<JSONObject> listOfModels = new ArrayList<>();
        for(Model m : models){
            listOfModels.add(m.toJson());
        }
        jsonObject.put("publications", listOfModels);
        return jsonObject;
    }
}
