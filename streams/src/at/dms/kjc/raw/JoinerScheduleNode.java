package at.dms.kjc.raw;

public class JoinerScheduleNode 
{
    public static final int FIRE = 0;
    public static final int RECEIVE = 1;
       
    public JoinerScheduleNode next;
    public int type;
    public String buffer;
    
    public int getType() 
    {
	return type;
    }
    
    public String getBuffer() 
    {
	return buffer;
    }

    public void printMe() 
    {
	if (type == FIRE)
	    System.out.print("Fire: ");
	else 
	    System.out.print("Receive: ");
	System.out.println(buffer);
    }

    public static void printSchedule(JoinerScheduleNode first) 
    {
	do {
	    first.printMe();
	    first = first.next;
	}while(first != null);
    }
    
}
