package aos;

public class Profiler {
	// Time measurement
	public static long startTimeNano;
	public static long endTimeNano;
	
	public static Profiler instance = null;
	
	private Profiler(){
		
	}
	
	public static Profiler getInstance(){
		if(instance == null){
			instance = new Profiler();
		}
		return instance;
	}
	
	public void startAnalyzing(){
		startTimeNano = System.nanoTime();
	}
	
	public void endAnalyzing(){
    	endTimeNano = System.nanoTime();
	}
	
	public void displayAnalysisResult(){
    	long duration = (endTimeNano - startTimeNano) / 1000000;  //divide by 1000000 to get milliseconds.
    	System.out.println(String.format("Execution time %d", duration));
	}
}
