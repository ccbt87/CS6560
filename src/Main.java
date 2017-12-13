/**
 * CS 6560
 * Project #1
 * Name: Hongjie Zhu
 * NetID: hf6233
 * OS: Windows 10
 * IDE: Eclipse Luna
 */

import java.io.IOException;
import java.util.Scanner; 

public class Main {
	FileSystem fs;
	
	public Main() {
		fs = new FileSystem(10);
	}
	
	void cmd(String[] args) throws IOException {
		if(args.length > 0) {
			switch(args[0].toUpperCase()) {
			case "CREATE":
				if(args.length == 3) {
					fs.create(args[1].toUpperCase().charAt(0), args[2]);
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "OPEN":
				if(args.length == 3) {
					fs.open(args[1].toUpperCase().charAt(0), args[2]);
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "CLOSE":
				fs.close();
				break;
			case "DELETE":
				if(args.length == 2) {
					fs.delete(args[1]);
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "READ":
				if(args.length == 2) {
					fs.read(Integer.parseInt(args[1]));
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "WRITE":
				if(args.length >= 3) {
					if(args[2].charAt(0) == '\'' && args[args.length-1].charAt(args[args.length-1].length()-1) == '\'') {
						String data = "";
						for(int i = 2; i < args.length; i++) {
							data = data + " " + args[i];
						}
						fs.write(Integer.parseInt(args[1]), data.substring(2, data.length()-1).toCharArray());
					}
					else {
						System.out.println("Make sure data is in single quotes!");
					}
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "SEEK":
				if(args.length == 3) {
					fs.seek(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			case "SAVE":
				fs.saveDiskImage();
				break;
			case "LOAD":
				fs.loadDiskImage();
				break;
			case "LS":
				fs.ls();
				break;
			case "LSALL":
				fs.lsAll();
				break;
			case "CD":
				if(args.length == 2) {
					fs.cd(args[1]);
				}
				else {
					System.out.println("Missing or having extra parameters!");
				}
				break;
			default:
				System.out.println("Invalid command!"); // invalid command
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Main simulator = new Main();
		Scanner scanner = new Scanner(System.in);
		do {
			System.out.print("> ");
			String str = scanner.nextLine();
			args = str.split("\\s+");
			if(args[0].toUpperCase().equals("EXIT") ) {
				break;
			}
			simulator.cmd(args);
		} while(true);
		scanner.close();
	}

}
