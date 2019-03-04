package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.*;
import java.text.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FileUploader extends Thread{
  private final String file_name;
  private final String destination;
  private final String sender;
  private final String group;
  private final Channel channel;
  private final String date;
  private final String time;
  
  public FileUploader(String file_name, String destination, String sender, Channel channel, String date, String time, String group){
      this.file_name = file_name;
      this.destination = destination;
      this.sender = sender;
      this.group = group;
      this.channel = channel;
      this.date = date;
      this.time = time;
        start();
    }
  
  public void run(){
      try{
      //Obtendo arquivo
      Path file_path = Paths.get(this.file_name);
      byte[] file = Files.readAllBytes(file_path);
      
      
      //Criando Mensagem
     MensagemProto.Mensagem.Builder pacote = MensagemProto.Mensagem.newBuilder();
     pacote.setEmissor(this.sender);
     pacote.setData(this.date);
     pacote.setHora(this.time);
     pacote.setGrupo(this.group);
     
     //Criando Conteudo
     MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
     ByteString bs = ByteString.copyFrom(file);
     
     conteudo.setCorpo(bs);
     conteudo.setTipo(Files.probeContentType(file_path));
     conteudo.setNome((file_path.getFileName()).toString());
     
     pacote.setConteudo(conteudo);
     
     //Obtendo Mensagem
     MensagemProto.Mensagem msg = pacote.build();
     byte[] buffer = msg.toByteArray();
     String dest = ""; //armazena destinatario
     
     if((this.group).equals("") == true){//se nao for para grupo
       (this.channel).basicPublish("", this.destination + "F", null, buffer);
       dest = "@" + this.destination;
      }
     else {
         (this.channel).basicPublish(this.group+"F", "", null, buffer);
         dest = "#" + this.destination;
     }
      
      
      System.out.println("\nArquivo \"" + this.file_name + "\" foi enviado para " + dest + " !");
      System.out.print(Chat.user_Destination + ">>");
      
      }catch(Exception e){
        System.out.println("Houve um problema no envio!");
        e.printStackTrace();
      
      }
  }
}