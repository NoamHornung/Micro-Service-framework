package bgu.spl.mics.application.objects;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Passive object representing the cluster.
 * <p>
 * This class must be implemented safely as a thread-safe singleton.
 * Add all the fields described in the assignment as private fields.
 * Add fields and methods to this class as you see fit (including public methods and constructors).
 */
public class Cluster {
	private Collection<GPU> GPUS;
	private Collection<CPU> CPUS;
	private ConcurrentHashMap<DataBatch, GPU> findGPU;
	private LinkedBlockingQueue<DataBatch> unprocessed;
	private ConcurrentHashMap<GPU, LinkedBlockingQueue<DataBatch>> processed;
	//statistics:
	private ArrayList<String> ModelsNames;
	private int numOfProcessedByCPU;
	private int CPUTimeUnitsUsed;
	private int GPUTimeUnitsUsed;

	private final Object lock_numOfProcessedByCPU = new Object();
	private final Object lock_CPUTimeUnitsUsed = new Object();
	private final Object lock_GPUTimeUnitsUsed = new Object();
	private final Object lock_ModelsNames = new Object();


	private static class SingletonHolder{
		private static Cluster instance= new Cluster();
	}

	/**
	 * Retrieves the single instance of this class.
	 */
	public static Cluster getInstance() {
		return Cluster.SingletonHolder.instance;
	}

	private Cluster(){
		numOfProcessedByCPU=0;
		CPUTimeUnitsUsed=0;
		GPUTimeUnitsUsed=0;
		ModelsNames= new ArrayList<>();
		findGPU= new ConcurrentHashMap<>();
		processed= new ConcurrentHashMap<>();
		unprocessed= new LinkedBlockingQueue<>();

	}
	public void addCPUSandGPUS(Collection<GPU> GPUS, Collection<CPU> CPUS){
		this.CPUS= CPUS;
		this.GPUS= GPUS;
		for(GPU gpu: GPUS){
			processed.put(gpu, new LinkedBlockingQueue<>());
		}
	}

	public void receiveDataBatchFromGPU(DataBatch dataBatch, GPU gpu){
		unprocessed.add(dataBatch);
		findGPU.put(dataBatch, gpu);
	}

	public DataBatch sendDataBatchToGPU(GPU gpu){
		return processed.get(gpu).poll();
	}

	public void receiveDataBatchFromCPU(DataBatch dataBatch){
		GPU gpu= findGPU.get(dataBatch);
		if(gpu != null) {
			processed.get(gpu).add(dataBatch);
			synchronized (lock_numOfProcessedByCPU) {
				numOfProcessedByCPU++;
			}
		}
	}
	public DataBatch sendDataBatchToCPU(){
		return unprocessed.poll();
	}

	public ArrayList<String> getModelsNames() {
		return ModelsNames;
	}

	public int getNumOfProcessedByCPU() {
		return numOfProcessedByCPU;
	}

	public int getCPUTimeUnitsUsed() {
		return CPUTimeUnitsUsed;
	}

	public int getGPUTimeUnitsUsed() {
		return GPUTimeUnitsUsed;
	}

	public void setCPUTimeUnitsUsed() {
		synchronized (lock_CPUTimeUnitsUsed) {
			CPUTimeUnitsUsed++;
		}
	}

	public void setGPUTimeUnitsUsed() {
		synchronized (lock_GPUTimeUnitsUsed) {
			GPUTimeUnitsUsed++;
		}
	}

	public synchronized void setModelsNames(String name){
		synchronized (lock_ModelsNames) {
			ModelsNames.add(name);
		}
	}

	public void addProcessed(DataBatch dataBatch, GPU gpu){ //for tests
		processed.get(gpu).add(dataBatch);
	}

	public void clear(){
		numOfProcessedByCPU=0;
		CPUTimeUnitsUsed=0;
		GPUTimeUnitsUsed=0;
		ModelsNames= new ArrayList<>();
		findGPU= new ConcurrentHashMap<>();
		processed= new ConcurrentHashMap<>();
		unprocessed= new LinkedBlockingQueue<>();
		//CPUS.clear();
		//GPUS.clear();
	}
}