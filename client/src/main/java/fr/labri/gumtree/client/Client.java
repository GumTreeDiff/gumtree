package fr.labri.gumtree.client;

import java.io.IOException;

public abstract class Client {
    @SuppressWarnings("unused")
    public Client(String[] args) {}

    public abstract void run() throws IOException;
}
