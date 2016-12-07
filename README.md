# AntWorld
In groups of 2, write a client that connects to the Ant World server and intelligently manages an ant colony.<br>

For grades of D, C, B, and A, you need to lose to, tie, beat or definitely beat the an unknown, instructor written AI of moderate smarts.<br>

Winning is defined as the score calculated from the total food your team has brought to the nest and your nest's ant population.<br>


To COMPILE:<br>
I did not include the .idea configure file since some of the setting are platform specific.<br>
After you create an IntelliJ project with the repo source, you will need to go into Project Structure and set<br>
Modules -> Sources -><br>
<ul>
   <li>Content Root: yourpath\AntWorld</li>
   <li>Source Folders: src</li>
   <li>Resource Folders: antworld.resources</li>
</ul>


Modules -> Dependencies -> <br>
<ul>
   <li>  1.8 (java)</li>
   <li>  Module source</li>
</ul>   <br>
   
To RUN:<br>
First, start the server:<br>
   antworld.server.AntWorld.main(String[] args)<br><br>
   
When the server is ready for a client it will display in the console:<br>
Server: socket opened on port 12321<br>
Server: waiting for client connection.....<br>

Then, start your client. To get started, you can use the sample client:<br>
  antworld.client.ClientRandomWalk.main(String[] args)<br>

  If args is null or length < 1, then the client will try to connect on localhost.
  Otherwise, args[0] will be used as the hostname.
  
  
