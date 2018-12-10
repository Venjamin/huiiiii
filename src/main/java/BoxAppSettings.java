public class BoxAppSettings {
    private String clientID;
    private String clientSecret;
    private String publicKeyID;
    private String privateKey;
    private String passphrase;
    private String enterpriseID;

    public BoxAppSettings() {
    }

    public BoxAppSettings(String clientID, String clientSecret, String publicKeyID, String privateKey, String passphrase, String enterpriseID) {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.publicKeyID = publicKeyID;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
        this.enterpriseID = enterpriseID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setPublicKeyID(String publicKeyID) {
        this.publicKeyID = publicKeyID;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setEnterpriseID(String enterpriseID) {
        this.enterpriseID = enterpriseID;
    }

    String getClientID() {
        return clientID;
    }

    String getClientSecret() {
        return clientSecret;
    }

    String getPublicKeyID() {
        return publicKeyID;
    }

    String getPrivateKey() {
        return privateKey;
    }

    String getPassphrase() {
        return passphrase;
    }

    String getEnterpriseID() {
        return enterpriseID;
    }
}
