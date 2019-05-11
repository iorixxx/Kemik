package edu.anadolu;

import java.nio.file.Path;

class AOF extends Kemik {

    static final String[] categories = new String[]{

            "EDB",
            "PZL",
            "TAR",
            "ILH",
            "ADL",
            "TRM",
            "WTK",
            "ILT",
            "ULI",
            "BSI",
            "MLY",
            "TRZ",
            "CMH",
            "MUH",
            "EID",
            "LBV",
            "KUL",
            "SHZ",
            "OMB",
            "LOJ",
            "PSI",
            "KYT",
            "CEK",
            "TSH",
            "FOT",
            "KIM",
            "SYT",
            "EMY",
            "MEI",
            "ARA",
            "KMT",
            "MUZ",
            "MAI",
            "YYT",
            "SAG",
            "EVI",
            "MIT",
            "ARK",
            "YAB",
            "SAK",
            "MAT",
            "SAN",
            "SIY",
            "TKY",
            "ARY",
            "BYA",
            "MVU",
            "RTP",
            "ASC",
            "TDE",
            "CBS",
            "SNT",
            "GKA",
            "TLT",
            "HUK",
            "CGE",
            "ISL",
            "KOI",
            "BIL",
            "EST",
            "FIN",
            "IST",
            "ECH",
            "MSP",
            "YBS",
            "FEL",
            "ISY",
            "TIC",
            "TUR",
            "HIT",
            "OKO",
            "OGK",
            "IKT",
            "TAB",
            "SOS",
            "IKY",
            "PMY"
    };

    private final Path p;

    AOF(Path p) {
        super(p);
        this.p = p;
    }

    @Override
    public String id() {
        String fileName = p.getFileName().toString();
        return fileName.substring(0, fileName.length() - 4);
    }

    @Override
    public String category() {
        String fileName = p.getFileName().toString();
        return fileName.substring(0, 3);
    }
}
