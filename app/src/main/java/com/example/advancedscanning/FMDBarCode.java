package com.example.advancedscanning;

public class FMDBarCode {
    private String gtin;
    private String batch;
    private String serial;
    private String expiry;

    @Override
    public String toString() {
        return "FMDBarCode{" +
                "gtin='" + gtin + '\'' +
                ", batch='" + batch + '\'' +
                ", serial='" + serial + '\'' +
                ", expiry='" + expiry + '\'' +
                '}';
    }

    public String getGtin() {
        return gtin;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }

    public static FMDBarCode buildFromGS1Data(String gs1){
        FMDBarCode bc = new FMDBarCode();
        return processGS1Data(bc, gs1);
    }

    private static FMDBarCode processGS1Data(FMDBarCode bc, String gs1){
        if (gs1.length() == 0)
            return bc;
        System.out.println("Unparsed Data Set: " + gs1);
        if (gs1.substring(0,2).equals("01")){
            System.out.println("GTIN detected");
            bc.setGtin(gs1.substring(2, 16));
            System.out.println(bc.getGtin());
            return processGS1Data(bc, gs1.substring(16));

        }else if (gs1.substring(0,2).equals("17")){
            System.out.println("Expiry detected");
            bc.setExpiry(gs1.substring(2, 8));
            System.out.println(bc.getExpiry());
            return processGS1Data(bc, gs1.substring(8));

        }else if (gs1.substring(0,2).equals("10")){
            System.out.println("Batch detected");
            int igs = getIndexOfGSChar(gs1, 20);
            int endIndex = gs1.length();
            endIndex = endIndex < 22 ? endIndex : 22;
            if (igs > -1) {
                endIndex = igs;
            }

            bc.setBatch(gs1.substring(2, endIndex));
            System.out.println(bc.getBatch());
            endIndex = igs + 1 > endIndex ? igs + 1 : endIndex;
            return processGS1Data(bc, gs1.substring(endIndex));
        }else if (gs1.substring(0,2).equals("21")){
            System.out.println("Serial detected");
            int igs = getIndexOfGSChar(gs1, 20);
            int endIndex = gs1.length();
            endIndex = endIndex < 22 ? endIndex : 22;
            if (igs > -1) {
                endIndex = igs;
            }
            bc.setSerial(gs1.substring(2, endIndex));
            System.out.println(bc.getSerial());
            endIndex = igs + 1 > endIndex ? igs + 1 : endIndex;
            return processGS1Data(bc, gs1.substring(endIndex));
        }
        return bc;

    }

    private static int getIndexOfGSChar(String s, int testLen){
        int i = s.indexOf((char)29);
        if (i > testLen) {
            i = -1;
        }
        return i;
    }
}