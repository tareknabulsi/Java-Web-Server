import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;

public class MyWebServer {
	
	public static void main(String args[]) throws IOException{
		int q_len = 6;
		int port = 2540;
		Socket socket;
		
		ServerSocket serverSocket = new ServerSocket(port, q_len);
		
		System.out.println("Tarek Nabulsi's web server starting up, "
				+ "listening at port 2540.\n");
		while (true){
			socket = serverSocket.accept(); 
			new Worker(socket).start(); 
		}
	}
}

class Worker extends Thread {
	Socket socket;
	
	Worker(Socket socket){
		this.socket = socket;
	}
	
	public void run(){
		BufferedReader input = null;
		PrintStream output = null;
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = new PrintStream(socket.getOutputStream());
			
			String request; //This will hold our GET request
			String socketData; //This will hold all other data
			String filePath = "./"; //initialize to home directory
			request = input.readLine(); //first line of input is GET request
			
			if (request != null)
				filePath = processRequest(request);
			
			//Print the rest of the stream input
			while (true){
		        socketData = input.readLine();
		        if (socketData == null || socketData.length() == 0){
		        	System.out.println("\n\n");
		        	System.out.flush();
		        	break;
		        }
		        else
		        	System.out.println(socketData);
		        System.out.flush();
		      }
			
			if (filePath.startsWith("cgi/addnums.fake-cgi"))
				addNums(filePath, output);
			else
				//Send page to browser
				sendFileToClient(filePath, output);
			
			input.close();
			output.close();
			socket.close();
			
		} catch (IOException e){
			System.out.println("Connection reset. Listening again...");
		}
	}
	

	/*
	 * Here we take the GET request and isolate the requested page into
	 * its own string. This is done by splitting the GET request by spaces,
	 * putting the results in a string array, then taking the actual file
	 * name from the array and putting it in a string variable. That string
	 * is offset by 1 to get rid of the forward slash character.
	 */
	static String processRequest(String request){
		String[] requestSplit;
		String fileLocation;
		
		//Take the first line of the input to process GET request
		requestSplit = request.split(" ");
		fileLocation = requestSplit[1].substring(1);
		
		//If request is empty or user tries to go to previous directory
		//we refer them to the default, which is the home page
		if (fileLocation.equals("") || fileLocation.startsWith("/"))
			fileLocation = "./";
		System.out.println(request);
		return fileLocation;
	}
	
	
	/*
	 * This function sends the file to the client (browser). After the 
	 * processRequest function creates a string containing the name of
	 * the file, it is then used to create a File object in this function. 
	 * If that File object ends up being being the home directory (./)
	 * or another directory, the function refers to getWorkingDirectory
	 * defined below, and stops. Otherwise it checks if the file exists, 
	 * and if it does, it calls another function, getType, to get the file
	 * type. It then tells the browser the content type and content length
	 * so the file content can be correctly formatted. Once that is complete,
	 * a Scanner object is created to read the file line by line, and print
	 * it over to the browser so the user can view the content.
	 */
	static void sendFileToClient(String fileLocation, PrintStream output){
		File file = new File(fileLocation);
		long size = file.length();
		
		if (fileLocation.equals("./") || file.isDirectory()){
			getWorkingDirectory(file, output);
			return;
		}
		if( !file.exists()){
		  output.println("You've found yourself in 404-ville");
		  return;
		}
		//Content type and data is printed to browser
		output.print("HTTP/1.0 200 OK\r\n" +
                "Content-Type: " + getType(fileLocation) + "\r\n" +
				"Content-Length: " + size + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: FileServer 1.0\r\n\r\n");
		
		//Scan file line by line to send to browser
		Scanner scanFile = null;
		try{
			scanFile = new Scanner(file);
			
			while(scanFile.hasNext()){
			  String text = scanFile.nextLine();
			  output.println(text);
			}
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
		scanFile.close();
	}
	
	
	/*
	 * Checks the end of the string name to determine whether the file
	 * is .txt or .html.
	 */
	static String getType(String fileLocation){
		String type;
		if (fileLocation.endsWith(".txt"))
			type = "text/plain";
		else if (fileLocation.endsWith(".html"))
			type = "text/html";
		else
			type = "";
		return type;
	}
	
	
	/*
	 * Get an array of all of the files and directories that exist in the
	 * file path that was specified in the GET request. After this function
	 * does the parsing to get the specific file or folder name, it will
	 * call another function to handle the array of names. That array will 
	 * then be used to generate the HTML text and hot links that will allow 
	 * the user to click them on the browser.
	 */
	static void getWorkingDirectory(File directory, PrintStream output){
		
	    //Array of all subfiles which will be converted to string form
	    File[] filesAndDirectories = directory.listFiles();
	    String[] strFilesAndDirectories = new String[filesAndDirectories.length];
	    
	    //Separate directories from files. Does not print if file is hidden
	    for (int i = 0; i < filesAndDirectories.length; i++){
	    	if (!filesAndDirectories[i].toString().startsWith(directory + "/.")){
	    		strFilesAndDirectories[i] = (
	    				filesAndDirectories[i].toString().replace(directory.toString() + "/", "")
	    				);
	    	}
	    }
	    
	    displayDirectory(directory, strFilesAndDirectories, output);
	}

	/*
	 * Here we create the HTML that displays all of the files in the
	 * directory, and we print it all to the server. The listed files are
	 * also hot links that take the viewer to the corresponding file.
	 * The argument called directory is used to remember the current working
	 * directory that the user is viewing on the browser. We then take the
	 * most recent directory from the whole working directory to use in
	 * the hot link. The current directory is added with the file name
	 * to produce an appropriate extension to add to the current working
	 * directory, thus bringing the viewer to the correct address.
	 */
	static void displayDirectory(File directory, String[] files, PrintStream output) {
		//We have to split this string to find the current working directory
		//The current working directory is provided to the hot links
		String dir = directory.toString();
		String[] dirs;
		String currentDir;
		dirs = dir.split("/");
		currentDir = dirs[dirs.length - 1];
		
		//Content type and data is printed to browser
		output.print("HTTP/1.0 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: FileServer 1.0\r\n\r\n");
		
		output.print("<pre>");
		
		//Only print title page if it is the home page
		if (directory.toString().equals("."))
			output.print("<h1> Tarek Nabulsi's Server Homepage </h1>");
		else
			output.print("<a href=\"/\">Go To Homepage</a> <br><br>");
		
		//Creating and printing the hot links to the browser
		for (int i = 1; i < files.length; i++){
			if (files[i] != null)
				output.print("<a href=\"" + currentDir + "/" + files[i] + "\">" 
							+ "<font face=\"verdana\" size=\"5\">"
							+ files[i] + "</a> <br><br>");
		}
	}
	
	
	/*
	 * This function works in a special case that is called when a specific
	 * GET request is received. This is meant to simulate a program being
	 * run on a website. addNums parses the GET request to get the name,
	 * first integer and the second integer. It then formats a response in
	 * HTML that gets sent to the browser so the user can see the result of
	 * their input.
	 */
	private void addNums(String filePath, PrintStream output){
		//The below two string variables are used to parse the data from GET
		String subString;
		String[] values;
		String result; //Will contain final formatted HTML response
		String name; //User's name
		int num1;
		int num2;
		int size; //Will store the size of result
		
		subString = filePath.replace("cgi/addnums.fake-cgi", "");
		subString = subString.replace("=", " ");
		subString = subString.replace("&", " ");
		values = subString.split(" ");
		
		//Storing the values from the split sub-string
		name = values[1];
		num1 = Integer.parseInt(values[3]);
		num2 = Integer.parseInt(values[5]);
		
		//Print to console as well
		System.out.println(name + ", the sum of " + String.valueOf(num1) +
						   " and "+ String.valueOf(num2) + 
							 " = " + String.valueOf(num1 + num2));
		
		//HTML formatted text of name, the two numbers and their sum
		result = "<font face = \"verdana\" size = \"6\" color = \"DarkCyan\">" +
				 "<h1>Hello " + name + "</h1>" +
				 "<p>The sum of " + String.valueOf(num1) + 
				 " and " + String.valueOf(num2) + 
				 " = " + String.valueOf(num1 + num2) +".</p><br>";
		
		//Size of the HTML statement
		size = result.length();
		
		//Content type and data is printed to browser
		output.print("HTTP/1.0 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
				"Content-Length: " + size + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: FileServer 1.0\r\n\r\n");
		
		//Printing the final results to the browser
		output.print("<pre>");
		output.print(result);
	}
}
