package ch.ethz.inf.vs.android.siwehrli.a3;

public class TextMessage implements Comparable<TextMessage>{
	public String message;
	public int timestamp;
	
	public TextMessage(String message, int timestamp){
		this.message=message;
		this.timestamp=timestamp;
	}

	public String getFormatedMessage() {
		return message;
	}

	public String getFormatedTime() {
		return timestamp +"";
	}

	@Override
	public int compareTo(TextMessage another) {
		return this.timestamp-another.timestamp;
	}
}
