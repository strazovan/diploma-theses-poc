package cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq;


public interface MessageBoxClient {

    void askForJob();

    void sendResult(String jobId, byte[] result);
}
