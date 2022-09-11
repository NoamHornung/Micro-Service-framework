package bgu.spl.mics.application;

//import jdk.nashorn.internal.parser.JSONParser;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBus;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

/** This is the Main class of Compute Resources Management System application. You should parse the input file,
 * create the different instances of the objects, and run the system.
 * In the end, you should output a text file.
 */
public class CRMSRunner {
    public static void main(String[] args) throws FileNotFoundException {
        LinkedList<Student> studentList = new LinkedList<>();
        LinkedList<GPU> GPUS = new LinkedList<>();
        LinkedList<CPU> CPUS = new LinkedList<>();
        LinkedList<ConferenceInformation> conferenceInformations = new LinkedList<>();
        int tickTime = 0;
        int duration = 0;
        Cluster cluster = Cluster.getInstance();
        try {
            Object obj = new JSONParser().parse(new FileReader(args[0]));
            JSONObject jsonObject = (JSONObject) obj;
            JSONArray Students = (JSONArray) jsonObject.get("Students");
            for (int i = 0; i < Students.size(); i++) {
                JSONObject student = (JSONObject) Students.get(i);
                String StudentName = (String) student.get("name");
                String department = (String) student.get("department");
                String Stringstatus = (String) student.get("status");
                Student.Degree status;
                if (Objects.equals(Stringstatus, "MSc")) {
                    status = Student.Degree.MSc;
                } else {
                    status = Student.Degree.PhD;
                }
                ArrayList<Model> models = new ArrayList<>();
                JSONArray JSONmodels = (JSONArray) student.get("models");
                for (int j = 0; j < JSONmodels.size(); j++) {
                    JSONObject model = (JSONObject) JSONmodels.get(j);
                    String ModelName = (String) model.get("name");
                    String stringType = (String) model.get("type");
                    Data.Type type;
                    if (Objects.equals(stringType, "Images")) {
                        type = Data.Type.Images;
                    } else if (Objects.equals(stringType, "Text")) {
                        type = Data.Type.Text;
                    } else {
                        type = Data.Type.Tabular;
                    }
                    long l = (long) model.get("size");
                    int size = (int) l;
                    models.add(new Model(ModelName, new Data(type, size)));
                }
                Student s = new Student(StudentName, department, status, models);
                for (Model m : models) {
                    m.setStudent(s);
                }
                studentList.add(s);
            }
            JSONArray JSONGPUS = (JSONArray) jsonObject.get("GPUS");
            for (int i = 0; i < JSONGPUS.size(); i++) {
                String stringType = (String) JSONGPUS.get(i);
                GPU.Type type;
                if (Objects.equals(stringType, "RTX3090")) {
                    type = GPU.Type.RTX3090;
                } else if (Objects.equals(stringType, "RTX2080")) {
                    type = GPU.Type.RTX2080;
                } else {
                    type = GPU.Type.GTX1080;
                }
                GPUS.add(new GPU(type, cluster));
            }
            JSONArray JSONCPUS = (JSONArray) jsonObject.get("CPUS");
            for (int i = 0; i < JSONCPUS.size(); i++) {
                long Lcores = (long) JSONCPUS.get(i);
                int cores = (int) Lcores;
                CPUS.add(new CPU(cores, cluster));
            }
            JSONArray Conferences = (JSONArray) jsonObject.get("Conferences");
            for (int i = 0; i < Conferences.size(); i++) {
                JSONObject conference = (JSONObject) Conferences.get(i);
                String name = (String) conference.get("name");
                long l = (long) conference.get("date");
                int date = (int) l;
                conferenceInformations.add(new ConferenceInformation(name, date));
            }
            long l = (long) jsonObject.get("TickTime");
            tickTime = (int) l;
            long l1 = (long) jsonObject.get("Duration");
            duration = (int) l1;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        cluster.addCPUSandGPUS(GPUS, CPUS);
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (CPU cpu : CPUS) {
            String name = "CPUService " + cpu.getCores();
            executorService.execute(new CPUService(name, cpu));
        }
        for (GPU gpu : GPUS) {
            String name = "GPUService " + gpu.getType();
            executorService.execute(new GPUService(name, gpu));
        }
        for (ConferenceInformation conference : conferenceInformations) {
            String name = "ConferenceInformation" + conference.getName();
            executorService.execute(new Thread(new ConferenceService(name,conference)));
        }
        for (Student s : studentList) {
            String name = "StudentService " + s.getName();
            executorService.execute(new StudentService(name, s));
        }
        executorService.execute(new TimeService(tickTime, duration));
        executorService.shutdown();
        try {
            executorService.awaitTermination((duration + 5000), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        JSONObject jsonObject = buildJson(studentList, conferenceInformations, cluster, tickTime);
        try {
            FileWriter fileWriter = new FileWriter("./src/main/file.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement je = JsonParser.parseString(jsonObject.toJSONString());
            String prettyJsonStr = gson.toJson(je);
            fileWriter.write(prettyJsonStr);
            fileWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    static JSONObject buildJson(LinkedList<Student> students, LinkedList<ConferenceInformation> conferences,
                                Cluster cluster, int tickTime) {
        JSONObject jsonObject = new JSONObject();
        LinkedList<JSONObject> listOfStudents = new LinkedList<>();
        for (Student s : students) {
            listOfStudents.add(s.toJson());
        }
        jsonObject.put("students", listOfStudents);
        LinkedList<JSONObject> listOfConferences = new LinkedList<>();
        for (ConferenceInformation c : conferences) {
            listOfConferences.add(c.toJson());
        }
        jsonObject.put("conferences", listOfConferences);
        jsonObject.put("cpuTimeUsed", cluster.getCPUTimeUnitsUsed());
        jsonObject.put("gpuTimeUsed", cluster.getGPUTimeUnitsUsed());
        jsonObject.put("batchesProcessed", cluster.getNumOfProcessedByCPU());
        return jsonObject;
    }
}


