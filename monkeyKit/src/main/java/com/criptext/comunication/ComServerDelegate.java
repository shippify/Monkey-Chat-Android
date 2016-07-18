package com.criptext.comunication;



//import juego.protocol.Message;


/*
 * GameProxy: es usada para implementarla como un intermediario entre el view screen que contiene la logica de lo que se est� manejando en ese momento
 * con los objetos de usuario y mensajes desde el RocketServer.
 * 
 * */
public interface ComServerDelegate {
    void handleMessage(ComMessage message);
    void handleEvent(short evid);
    void promptAction(String action);
    void disconnected();
}
