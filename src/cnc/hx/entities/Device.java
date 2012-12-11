package cnc.hx.entities;

public class Device {
	
	String name, ip;
	
	public String getName() {
		return name;
	}

	public Device setName(String name) {
		this.name = name;
		return this;
	}

	public Device setIp(String ip) {
		this.ip = ip;
		return this;
	}

	public String getIp() {
		return ip;
	}
	
}
