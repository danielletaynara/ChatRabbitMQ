package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.*;
import java.text.*;
import java.lang.*;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File; 


public class Chat {
    
  private static final DateFormat DATA = new SimpleDateFormat("dd/MM/yyyy");//Data
  private static final DateFormat HORA = new SimpleDateFormat("HH:mm");//HORA
  static String user_Destination = ""; //Guarda nome do destino das msgs
  static Calendar cal = null;//calendario
    

  public static void main(String[] argv) throws Exception {
    
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUri("amqp://danielle:danielle@LB-HTTPP-3096cedfd3b48080.elb.us-east-1.amazonaws.com");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel(); // canal para mensagens
    Channel channel_file = connection.createChannel(); //canal para fila de arquivos
    
    Scanner scan = new Scanner(System.in);
    
    System.out.print("USER: ");
        final String user_Queue = scan.nextLine();
    channel.queueDeclare(user_Queue, false, false, false, null);
    createDir(user_Queue); //cria pasta para guardar downloads do usuario
    String user_Queue_files = user_Queue + "F";
    channel.queueDeclare(user_Queue, false, false, false, null);//cria fila
    channel_file.queueDeclare(user_Queue_files, false, false, false, null);//cria fila de arquivos 
    
    String message = ""; //Guardar msg do usuario
    
    Consumer consumer = new DefaultConsumer(channel) {
        
     @Override

        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
           System.out.println("");
           try{
            MensagemProto.Mensagem m = MensagemProto.Mensagem.parseFrom(body);
            System.out.print("(" + m.getData() + " às " + m.getHora() + ") " );
            if(m.getGrupo() != ""){
                System.out.print(m.getEmissor() + "#" + m.getGrupo() + " diz: ");
            }else{
                System.out.print(m.getEmissor() + " diz: ");
            }
            System.out.println(m.getConteudo().getCorpo().toStringUtf8());
           }catch (Exception e) {System.out.println("ERRO!");}
           System.out.print(user_Destination + ">>");
        }
    };
  channel.basicConsume(user_Queue, true, consumer); 
  channel_file.basicConsume(user_Queue_files, true, consumer);
  
  
  //ENVIO
  
  String grupo = "";
    while(message.equals(".") == false){
        System.out.print(user_Destination + ">>");
        message = scan.nextLine();
        
        if(message.equals(".") == true) break; //comando de saida
        
        if(message.charAt(0) == '@'){//Muda usuario do prompt
           user_Destination = message;
           grupo = "";
        } else
        if(message.charAt(0) == '#'){//mensagem para grupo
            user_Destination = message;
            grupo =user_Destination.substring(1);
        } else if(message.charAt(0) == '!') {//verifica comando
            if(message.contains("addGroup")){
               addGroup(message, channel, user_Queue);
            } else if(message.contains("addUser")){//add usuario a grupo
                   addUser(message, channel);
            } else if(message.contains("delFromGroup")){//deletar usuario do grupo
                   delFromGroup(message, channel);
            } else if(message.contains("removeGroup")){//remover grupo
                  removeGroup(message, channel);
            } else if(message.contains("upload")){ // faz upload de um arquivo
                  String destination = user_Destination.substring(1); //remove o '@' ou '#'
                  String fileName = message.substring(8);
                  System.out.println("Enviando \"" + fileName + "\" para " + user_Destination + ".");
                  uploadFile(fileName, destination, channel_file, user_Queue, grupo);
            }
            
        } else //Envia mensagem
         if(user_Destination.equals("") == false)
             {  
                if(user_Destination.charAt(0) == '#')//caso seja para um grupo
                  sendMessage(user_Queue, message, "", channel, grupo);
                else sendMessage(user_Queue, message, user_Destination.substring(1), channel, ""); 
             }
        
    }

  
  channel.close();
  connection.close();


    
  }
  
  //Cria um grupo
  static void addGroup(String message, Channel channel, String criador) throws Exception{
      String groupName = message.substring(10); //pega nome do exchange
      channel.exchangeDeclare(groupName, "fanout");//criando exchange-grupo
      channel.queueBind(criador, groupName, ""); //Adiciona o usuario criador ao grupo.
  }
  
  
  //Adiciona um usuario a um grupo
  static void addUser(String message, Channel channel)throws Exception{
      String substr = message.substring(9);//pega restante da string, apos o comando addUser
      String user_name = substr.substring(0, substr.indexOf(" "));//pega nome usuario
      String groupName = substr.substring(substr.indexOf(" ") + 1);//obtem nome grupo
      channel.exchangeDeclare(groupName, "fanout");//criando exchange-grupo caso nao exista
      try{//caso a fila nao exista
      channel.queueBind(user_name, groupName, "");// faz o binding
      }catch (Exception e){}
  }
  
  //Remove usuario de um grupo
  static void delFromGroup(String message, Channel channel)throws Exception{
      String substr = message.substring(14);
      String user_name = substr.substring(0, substr.indexOf(" "));//pega nome usuario
      String groupName = substr.substring(substr.indexOf(" ") + 1);//obtem nome grupo
      try{//caso a fila nao exista
      channel.queueUnbind(user_name, groupName, "");// desfaz o binding
      }catch(Exception e){}
  }
  
  
  //Remove um grupo
  static void removeGroup(String message, Channel channel) throws Exception{
      String groupName = message.substring(13);
      channel.exchangeDelete(groupName);//removendo exchange-grupo
  }
  
  //enviar mensagem
  static void sendMessage(String user_Queue, String message, String user_Destination, Channel channel, String grupo) throws Exception{
     //Criando Mensagem
     MensagemProto.Mensagem.Builder pacote = MensagemProto.Mensagem.newBuilder();
     
     cal = Calendar.getInstance();//calendario. Obtem info
     pacote.setEmissor(user_Queue);
     pacote.setData( (DATA.format(cal.getTime())));
     pacote.setHora(HORA.format(cal.getTime()));
     pacote.setGrupo(grupo);
     
     //Criando Conteudo
     MensagemProto.Conteudo.Builder conteudo = MensagemProto.Conteudo.newBuilder();
     ByteString bs = ByteString.copyFrom(message.getBytes("UTF-8"));
     
     conteudo.setCorpo(bs);
     conteudo.setTipo("");
     conteudo.setNome("");
     
     pacote.setConteudo(conteudo);
     
     //Obtendo Mensagem
     MensagemProto.Mensagem msg = pacote.build();
     byte[] buffer = msg.toByteArray();
     
     channel.basicPublish(grupo, user_Destination, null, buffer);
  }
  
  //Recebe um pacote e salva-o, se for arquivo, ou converte-o para string, se for texto.
  static String getMessage(byte[] pacote, String user) throws Exception{
       MensagemProto.Mensagem msg = MensagemProto.Mensagem.parseFrom(pacote);
       String emissor = msg.getEmissor();
       String data = msg.getData();
       String hora = msg.getHora();
       String grupo = msg.getGrupo();
       
       MensagemProto.Conteudo conteudo = msg.getConteudo();
       String nome = conteudo.getNome();
       String r = ""; //retorno
       
       if(nome.equals("") == false){//se for um arquivo
           FileDownloader downloader = new FileDownloader(user,conteudo, emissor, data, hora, grupo);
       } else{
           ByteString corpo = conteudo.getCorpo();
           String info = corpo.toStringUtf8();
           
           if(grupo.equals("") == false) grupo = "#" + grupo; //se msg for para um grupo;
           else emissor = "@" + emissor;
           
           r = "(" + data + " às " + hora + ") " + emissor + grupo + " diz: " + info; //caso seja para um grupo
       }
       return r;
  }
  
  
  //Faz upload do arquivo
  static void uploadFile(String file_name, String destination, Channel channel, String sender, String group){
      cal = Calendar.getInstance();//calendario. Obtem info
      String date = DATA.format(cal.getTime());
      String time = HORA.format(cal.getTime());
      FileUploader uploader = new FileUploader(file_name, destination, sender, channel, date, time, group);
  }
  
  //cria um diretorio
  static void createDir(String user){
      new File(user + "_downloads").mkdir();
  }



  
}
