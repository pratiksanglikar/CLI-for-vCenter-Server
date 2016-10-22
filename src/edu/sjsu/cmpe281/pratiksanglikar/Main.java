package edu.sjsu.cmpe281.pratiksanglikar;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.vmware.vim25.InvalidCollectorVersion;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Datastore;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.HostSystem;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.Network;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;

public class Main {

	private static ServiceInstance si = null;

	/**
	 * the main entry point of the program.
	 * @param args takes three command line arguments - IPAddress, username and password.
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	public static void main(String args[]) throws InvalidProperty, RuntimeFault, RemoteException {
		System.out.println("CMPE281 HW2 from Pratik Sanglikar\n");
		if (args.length < 3) {
			System.out.println("Incorrect use of arguments!");
			System.out.println("\tUsage: main <IPAdress> <Username> <Password>");
			System.exit(1);
		}
		try {
			connectToVM(args[0], args[1], args[2]);
		} catch (Exception e) {
			System.out.println("Unable to connect: " + e.getMessage());
			exitSafely();
		}
		listenToCommands();
	}

	/**
	 * the method listens to commands until user enters the 'exit' command.
	 * 
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	private static void listenToCommands() throws InvalidProperty, RuntimeFault, RemoteException {
		Scanner scanner = new Scanner(System.in);
		String input = null;
		while (true) {
			System.out.println("\npratik-021>");
			input = scanner.next();
			switch (input) {
			case "exit":
				scanner.close();
				exitSafely();
				break;
			case "help":
				printHelp();
				break;
			case "host":
				performHostAction(scanner.nextLine());
				break;
			case "vm":
				performVMAction(scanner.nextLine());
				break;
			default:
				System.out.println("Unrecognized command!\n\tType 'help' for list of all supported commands.");
			}
		}
	}

	/**
	 * this method performs actions related to Virtual Machine.
	 * 
	 * @param params vmname, action.
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	private static void performVMAction(String params) throws InvalidProperty, RuntimeFault, RemoteException {
		if (params == null || params.length() == 0) {
			enumerateAllVMs();
			return;
		}
		String vmname, action;
		StringTokenizer st = new StringTokenizer(params, " ");
		vmname = st.nextToken().trim();
		try{
			action = st.nextToken().trim();
		} catch(Exception e) {
			System.out.println("Unrecognized command!\n\tType 'help' for list of all supported commands.");
			return;
		}
		switch (action) {
		case "info":
			printVMInfo(vmname);
			break;
		case "on":
			powerOnVM(vmname);
			break;
		case "off":
			powerOffVM(vmname);
			break;
		case "shutdown":
			shutdownVM(vmname);
			break;
		default:
			System.out.println("Unrecognized command!\n\tType 'help' for list of all supported commands.");
			return;
		}
	}

	/**
	 * prints the information about the virtual machine specified by name - vmname
	 * @param vmname name of the virtual machine.
	 */
	private static void printVMInfo(String vmname) {
		VirtualMachine vm = getVirtualMachine(vmname);
		if(vm == null) { 
			System.out.println("\n VM " + vmname + " not found!");
			return;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("\n\tName = \t\t\t\t" + vm.getName() + "\n");
		sb.append("\tGuest Full Name = \t\t" + vm.getGuest().getGuestFullName() + "\n");
		sb.append("\tGuest State = \t\t\t" + vm.getGuest().getGuestState() + "\n");
		sb.append("\tIP Addr = \t\t\t" + vm.getGuest().getIpAddress() + "\n");
		sb.append("\tTool Running Status = \t\t" + vm.getGuest().getToolsRunningStatus() + "\n");
		sb.append("\tPower State = \t\t\t" + vm.getSummary().runtime.powerState.toString() + "\n");
		System.out.println(sb.toString());
	}

	/**
	 * power ons the Virtual Machine specified by vmname.
	 * @param vmname name of the virtual machine.
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	@SuppressWarnings("deprecation")
	private static void powerOnVM(String vmname) throws InvalidProperty, RuntimeFault, RemoteException {
		VirtualMachine vm = getVirtualMachine(vmname);
		if(vm == null) {
			System.out.println("\n VM " + vmname + " not found!");
			return;
		}
		Task powerOnTask = null;
		StringBuffer sb = new StringBuffer();
		try {
			powerOnTask = vm.powerOnVM_Task(null);
			if (powerOnTask.waitForMe() == Task.SUCCESS) {
				sb.append("\tName = " + vmname + "\nPower on VM: status = success, completion time = "
						+ powerOnTask.getTaskInfo().getCompleteTime().getTime());
			} else {
				sb.append("\tName = " + vmname + "\nPower on VM: status = " + powerOnTask.getTaskInfo().getState()
						+ ", completion time = " + powerOnTask.getTaskInfo().getCompleteTime().getTime());
			}
		} catch (RemoteException e) {
			sb.append("\tName = " + vmname + "\nPower on VM: status = " + powerOnTask.getTaskInfo().error.localizedMessage
					+ ", completion time = " + powerOnTask.getTaskInfo().getCompleteTime().getTime());
		}
		System.out.println(sb.toString());
	}

	/**
	 * Power offs the Virtual Machine specified by the vmname
	 * @param vmname name of the virtual machine.
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	@SuppressWarnings("deprecation")
	private static void powerOffVM(String vmname) throws InvalidProperty, RuntimeFault, RemoteException {
		VirtualMachine vm = getVirtualMachine(vmname);
		if(vm == null) {
			System.out.println("\n VM " + vmname + " not found!");
			return;
		}
		Task powerOffTask = null;
		StringBuffer sb = new StringBuffer();
		try {
			powerOffTask = vm.powerOffVM_Task();
			if (powerOffTask.waitForMe() == Task.SUCCESS) {
				sb.append("\tName = " + vmname + "\nPower off VM: status = success, completion time = "
						+ powerOffTask.getTaskInfo().getCompleteTime().getTime());
			} else {
				sb.append("\tName = " + vmname + "\nPower off VM: status = " + powerOffTask.getTaskInfo().getState()
						+ ", completion time = " + powerOffTask.getTaskInfo().getCompleteTime().getTime());
			}
		} catch (RemoteException e) {
			sb.append("\tName = " + vmname + "\nPower off VM: status = " + powerOffTask.getTaskInfo().error.localizedMessage
							+ ", completion time = " + powerOffTask.getTaskInfo().getCompleteTime().getTime());
		}
		System.out.println(sb.toString());
	}

	/**
	 * shut downs the guest OS on Virtual Machine specified by vmname.
	 * if an exception occurs or after the timeout of 3 minutes, the system power offs the virtual machine.
	 * @param vmname name of the virtual machine.
	 * @throws InvalidProperty
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	private static void shutdownVM(String vmname) throws InvalidProperty, RuntimeFault, RemoteException {
		VirtualMachine vm = getVirtualMachine(vmname);
		if(vm == null) {
			System.out.println("\n VM " + vmname + " not found!");
			return;
		}
		try {
			vm.shutdownGuest(); // initiate the shutdown of guest OS.
			pollServer(vm); // start polling the server for power off status. 
		} catch (RemoteException e) {
			System.out.println("\tName = " + vm.getName() + "Graceful shutdown failed. Now try a hard power off.\n");
			powerOffVM(vm.getName());
		}
	}

	/**
	 * Polls the server every two seconds for current power status.
	 * If VM is powered off then exits the loop else after the timeout of 
	 * 3 minutes, it powers off the VM.
	 * @param vm The vitual machine to poll the server for.
	 * @throws InvalidCollectorVersion
	 * @throws RuntimeFault
	 * @throws RemoteException
	 */
	private static void pollServer(VirtualMachine vm) throws InvalidCollectorVersion, RuntimeFault, RemoteException {
		long timeout = System.currentTimeMillis() + 180000; // set timeout to 3 minutes from current time. (180000 ms = 3 minutes)
		boolean shutdownCompleted = false;
		while (System.currentTimeMillis() <= timeout) { // try for 3 minutes.
			try {
				System.out.print(" . "); 
				if(isVMPoweredOff(vm.getName())) { // Check if the VM is powered off.
					System.out.println("\tName = " + vm.getName() + "Shutdown guest: completed, time = " + new Date()); 
					shutdownCompleted = true; // if VM powered off, print message and exit.
					break;
				}
				Thread.sleep(2000); // Sleep for 2 seconds until polling again to server.
			} catch (InterruptedException e) {
				
			}
		}
		if(!shutdownCompleted) {
			// If an exception occurs or timeout occurs, hard power off the machine.
			System.out.println("\tName = " + vm.getName() + "Graceful shutdown failed. Now try a hard power off.\n");
			powerOffVM(vm.getName());
		}
	}

	/**
	 * checks if the virtual machine is powered off.
	 * @param vmname
	 * @return
	 */
	private static boolean isVMPoweredOff(String vmname) {
		VirtualMachine vm = getVirtualMachine(vmname); // get the virtual machine
		if(vm == null) {
			System.out.println("\n VM " + vmname + " not found!");
			return false;
		}
		if(vm.getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOff)) { // If VM is powered off, return true.
			return true;
		}
		return false; // VM is not powered off yet!
	}
	
	/**
	 * enumerates through all virtual machines.
	 */
	private static void enumerateAllVMs() {
		Folder rootFolder = si.getRootFolder();
		ManagedEntity[] mes = null;
		StringBuffer sb = new StringBuffer();
		try {
			mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (mes == null || mes.length == 0) {
			return;
		}
		for (int i = 0; i < mes.length; i++) {
			sb.append("\tvm[" + i + "]\t Name = ");
			sb.append(((VirtualMachine) mes[i]).getName() + "\n");
		}
		System.out.println(sb.toString());
	}

	/**
	 * performs actions related to HostSystem.
	 * @param params hostname and action.
	 */
	private static void performHostAction(String params) {
		if (params == null || params.length() == 0) {
			enumerateAllHosts();
			return;
		}
		String hname, action;
		StringTokenizer st = new StringTokenizer(params, " ");
		hname = st.nextToken().trim();
		try{
			action = st.nextToken().trim();
		} catch(Exception e) {
			System.out.println("Unrecognized command!\n\tType 'help' for list of all supported commands.");
			return;
		}
		switch (action) {
		case "info":
			printHostInfo(hname);
			break;
		case "datastore":
			enumerateDatastores(hname);
			break;
		case "network":
			enumerateNetworks(hname);
			break;
		default:
			System.out.println("Unrecognized command!\n\tType 'help' for list of all supported commands.");
			return;
		}
	}

	/**
	 * prints the info about the host specified by hostname; 
	 * @param hname name of the host.
	 */
	private static void printHostInfo(String hname) {
		HostSystem host = getHost(hname);
		if(host == null) {
			System.out.println("\n Host " + hname + " not found!");
			return;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("\tName = \t\t\t" + host.getName() + "\n");
		sb.append("\tProduct Full Name = \t" + host.getConfig().getProduct().getFullName() + "\n");
		sb.append("\tCpu Cores = \t\t" + host.getHardware().getCpuInfo().getNumCpuCores() + "\n");
		sb.append("\tRAM = \t\t\t" + convertToGB(host.getHardware().getMemorySize()) + " GB\n");
		System.out.println(sb.toString());
	}

	/**
	 * lists all datastores of the host.
	 * @param hname
	 */
	private static void enumerateDatastores(String hname) {
		StringBuffer sb = new StringBuffer();
		HostSystem host = getHost(hname);
		if(host == null) {
			System.out.println("\n Host " + hname + " not found!");
			return;
		}
		sb.append("Name = " + hname + "\n");
		try {
			Datastore[] datastores = host.getDatastores();
			for (int i = 0; i < datastores.length; i++) {
				sb.append("\tDatastore[" + i + "]: name = " + datastores[i].getName() + ", capacity = "
						+ convertToGB(datastores[i].getInfo().getMaxVirtualDiskCapacity()) + " GB,");
				sb.append(" FreeSpace = " + convertToGB(datastores[i].getInfo().getFreeSpace()) + " GB\n");
			}
		} catch (InvalidProperty e) {
			e.printStackTrace();
		} catch (RuntimeFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println(sb.toString());
	}

	/**
	 * lists all networks of the host.
	 * @param hname
	 */
	private static void enumerateNetworks(String hname) {
		StringBuffer sb = new StringBuffer();
		HostSystem host = getHost(hname);
		if(host == null) {
			System.out.println("\n Host " + hname + " not found!");
			return;
		}
		sb.append("Name = " + hname + "\n");
		Network[] networks = null;
		try {
			networks = host.getNetworks();
			for (int i = 0; i < networks.length; i++) {
				sb.append("\tNetwork[" + i + "]: name = " + networks[i].getName() + "\n");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		System.out.println(sb.toString());
	}

	/**
	 * lists all hosts present.
	 */
	private static void enumerateAllHosts() {
		Folder rootFolder = si.getRootFolder();
		ManagedEntity[] mes = null;
		StringBuffer sb = new StringBuffer();
		try {
			mes = new InventoryNavigator(rootFolder).searchManagedEntities("HostSystem");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (mes == null || mes.length == 0) {
			return;
		}
		for (int i = 0; i < mes.length; i++) {
			sb.append("\thost[" + i + "] Name = ");
			sb.append(((HostSystem) mes[i]).getName() + "\n");
		}
		System.out.println(sb.toString());
	}

	/**
	 * prints the help.
	 */
	private static void printHelp() {
		System.out.format("%-30s%-60s", "\nexit", "Exit the program");
		System.out.format("%-30s%-60s", "\nhelp", "Print out the usage");
		System.out.format("%-30s%-60s", "\nhost", "Enumerate all hosts");
		System.out.format("%-30s%-60s", "\nhost hname info", "Show info of the host hostname");
		System.out.format("%-30s%-60s", "\nhost hname datastore", "Enumerate datastores of host hostname");
		System.out.format("%-30s%-60s", "\nhost hname network", "Enumerate networks of host hostname");
		System.out.format("%-30s%-60s", "\nvm", "Enumerate all virtual machines");
		System.out.format("%-30s%-60s", "\nvm vname info", "Show info of the VM vname");
		System.out.format("%-30s%-60s", "\nvm vname on", "Power on the VM vname");
		System.out.format("%-30s%-60s", "\nvm vname off", "Power off the VM vname");
		System.out.format("%-30s%-60s", "\nvm vname shutdown", "Shutdown the VM vname\n");
	}

	/**
	 * returns the instance of host for given hostname.
	 * @param hostName name of the host.
	 * @return
	 */
	private static HostSystem getHost(String hostName) {
		Folder rootFolder = si.getRootFolder();
		HostSystem host = null;
		try {
			host = (HostSystem) new InventoryNavigator(rootFolder).searchManagedEntity("HostSystem", hostName);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return host;
	}

	/**
	 * returns the instance of the Virtual Machine specified by vmname. 
	 * @param vmname
	 * @return
	 */
	private static VirtualMachine getVirtualMachine(String vmname) {
		Folder rootFolder = si.getRootFolder();
		VirtualMachine vm = null;
		try {
			vm = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", vmname);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return vm;
	}

	/**
	 * Logout from the server and terminate the program.
	 * 
	 */
	private static void exitSafely() {
		if (si != null) {
			si.getServerConnection().logout();
		}
		System.out.println("Exiting...");
		System.exit(0);
	}

	/**
	 * given number of bytes, returns equivalent Gigabytes representation.
	 * @param bytes
	 * @return GBs
	 */
	private static int convertToGB(long bytes) {
		return (int) (bytes / 1073741824);
	}

	/**
	 * Connects to vSphere server using provided credentials.
	 * @param ip
	 * @param username
	 * @param password
	 * @throws RemoteException
	 * @throws MalformedURLException
	 */
	private static void connectToVM(String ip, String username, String password)
			throws RemoteException, MalformedURLException {
		si = new ServiceInstance(new URL(ip), username, password, true);
		return;
	}
}

// vm Pratik-Sanglikar-ub1404-021 on