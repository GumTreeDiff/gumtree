package fr.labri.gumtree.client;

import java.io.IOException;

public abstract class Client {
    @SuppressWarnings("unused")
    public Client(String[] args) {}

    abstract public void run() throws IOException;
}
