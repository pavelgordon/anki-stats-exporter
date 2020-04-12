## Idea
SQlite db stored as file on my local machine.  
Server which can get stats from file is deployed at ankistats.pgordon.dev/  

How to sync between local and remote?

Two services - first reads sql database and puts this data in postgres table on remote
Second - deployed on remote, reads database and renders it