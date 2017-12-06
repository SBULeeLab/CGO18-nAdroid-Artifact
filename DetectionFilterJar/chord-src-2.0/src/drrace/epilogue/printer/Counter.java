package drrace.epilogue.printer;

/*
 * This is a counter with name and number
 */

public class Counter {
	private final String name;
	private final int num;
	
	public Counter(String name, int num){
		this.name = name;
		this.num = num;
	}
	
	public String getName(){
		return name;
	}
	
	public int getNum(){
		return num;
	}
}
