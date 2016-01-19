package com.overs.sailscore;

public class SailscoreHelpers {
	/* Method to convert result code into the text equivalent
	 * 
	 */
	
	public String convertResultCode (int resultCode) {
		String sResultCode = null;
		switch (resultCode) {
			case 0:  sResultCode = "";     break;
			case 1:  sResultCode = "DNC";  break;
			case 2:  sResultCode = "DNS";  break;
			case 3:  sResultCode = "DNF";  break;
			case 4:  sResultCode = "DNE";  break;
			case 5:  sResultCode = "OCS";  break;
			case 6:  sResultCode = "BFD";  break;
			case 7:  sResultCode = "RET";  break;
			case 8:  sResultCode = "DGM";  break;
			case 9:  sResultCode = "DSQ";  break;
			case 10: sResultCode = "Duty";  break;
			case 11: sResultCode = "RDG"; break;
			case 12: sResultCode = "RDGa"; break;
			case 13: sResultCode = "RDGb";  break;
			case 14: sResultCode = "SCP";  break;
			case 15: sResultCode = "ZFP";  break;
		}
		return sResultCode;
	}
	
	// and a method to convert a code in text back to a value
	public int codeToInt (String resultCode) {
		String[] codes = {"", "DNC", "DNS", "DNF", "DNE", "OCS", "BFD", "RET", "DGM",
		                  "DSQ", "Duty", "RDG", "RDGa", "RDGb", "SCP", "ZFP"};
		int i=0;
		//String result = resultCode.toString();
		while (resultCode != codes[i]) {
			i++;
		}
		return i;
		
	}
}
