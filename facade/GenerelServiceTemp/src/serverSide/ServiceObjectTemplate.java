

abstract class ServiceObjectTemplate implements Runnable{

    /**
     * The template method for following a certain algorithm
     */
    public void run () {
        openStreams();
        while (isOpenService()) {
            getInstruction();
            decodeAndExecuteInstruction();
            createReplyMsg();
            replyClient();
        }
        closeStreams();
    }

    abstract void openStreams();
    abstract boolean isOpenService();
    abstract void getInstruction();
    abstract void decodeAndExecuteInstruction();
    abstract void createReplyMsg();
    abstract void replyClient();
    abstract void closeStreams();


}
