#FLOW CHART


Client -> Server:

```
REQUEST my_song.mpg HTTP/1.1\r\n
\r\n
```

Server -> Client:

```
HTTP/1.1 OK 201\r\n
name: my_song.mpg\r\n
peer: 192.168.1.11\r\n
size: 232323232\r\n
\r\n
```

-- SERVER GOES BACK TO WAITING --


Client -> ClientServer:

```
GET my_song.mpg HTTP/1.1\r\n
\r\n
```

-- Client starts waiting for file --

ClientServer -> Client:

```
HTTP/1.1 OK 202\r\n
name: my_song.mp4\r\n
size: 2323232323\r\n
\r\n
FILE BYTES
```

-- Client Server goes back to waiting from below --

-- Client goes back to waiting from below --

~~~~~~~~~~~~~~~~~~~~~~~
Client joining network / refreshing list 

Client -> Server:

```
POST updList HTTP/1.1\r\n
name: size\r\n
\r\n
NO DATA
```

-- RDT gauranteed / expected don't implement / sorc appr --

Server -> Client:

```
HTTP/1.1 OK 200\r\n
song1.mp4: 2323232\r\n
\r\n
```

~~~~~~~~~~


~~~ Client has:
overall main:
	1 thread - dedicated for server communication - thread would be always open
	1 thread - GUI
	1 thread - outgoing thread
		- child thread for song
		-
		-
	1 thread - peer, size, name - > TCP connection with peer - once done getting entire file destroy
