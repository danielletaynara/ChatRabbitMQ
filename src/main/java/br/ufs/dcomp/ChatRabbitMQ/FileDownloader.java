package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.*;
import java.text.*;
import java.io.FileOutputStream;
import com.google.protobuf.ByteString;


public class FileDownloader extends Thread{
  private final String destino;
  private final MensagemProto.Conteudo conteudo;
  private final String emissor;
  private final String data;
  private final String hora;
  private final String grupo;
  
  public FileDownloader(String destino, MensagemProto.Conteudo conteudo, String emissor, String data, String hora, String grupo){
      this.destino = destino;
      this.conteudo = conteudo;
      this.emissor = emissor;
      this.data = data;
      this.hora = hora;
      this.grupo = grupo;
        start();
    }
  
  public void run(){
      try{
        ByteString corpo = (this.conteudo).getCorpo();
        byte[] buffer = corpo.toByteArray();
        String nome = (this.conteudo).getNome();
        
        //ESCREVENDO NO ARQUIVO
        FileOutputStream outputStream = new FileOutputStream(this.destino + "_downloads" + "/" + nome); 
        outputStream.write(buffer);
        outputStream.close();
        
        String emi = "";
        if((this.grupo).equals("") == false) {//se for para um grupo
          emi = this.emissor + "#" + this.grupo;
        }
        else {
          emi = "@" + this.emissor;
        }
        
        System.out.println("(" + this.data + " às " + this.hora + ") Arquivo " + "/" + nome + "/" + " recebido de " + emi + "  !");
        System.out.print(Chat.user_Destination + ">>");
        
      }catch(Exception e){
          System.out.println("Houve um problema no envio!");
      }
  
 }
 
}