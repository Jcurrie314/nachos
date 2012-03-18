# the purpose of this make file is to make
# pushing and pulling easier on when using git hub
#  
# NOTE: placeing "@" in front of command will stop console echoing
##################
NAME =Derrick Ho
USERNAME =wh1pch81n
EMAIL = wh1pch81n@gmail.com
REPOSITORY=nachos

add:
	@git add ./$(REPOSITORY)

commit:
	@echo "==Please enter your COMMENT. finish by hitting ENTER==";\
	read dCOM;\
	git commit -m "$$dCOM  --$(NAME)"

push:
	@git push -u origin master

pull:#this command will pull and merge
	@git pull
all:
	@make add commit push

#below commands used for set up.  Should only do this once
configname:
	git config --global user.name "$(NAME)"
configemail:
	git config --global user.email $(EMAIL)
init:
	git init
remote:
	git remote add origin git@github.com:$(USERNAME)/$(REPOSITORY).git

#ssh public private keys

# open ssh directory
openssh:
	cd ~/.ssh
createbackup:
	mkdir back_up
backup:
	id_rsa* ./back_up
remove:
	rm id_rsa*
gen:
	ssh-keygen -t rsa -C "$(EMAIL)" 

#it will ask you for a pass phrase.  Enter something that you
# will remember.  its like a password but better.  The better
# part is done under the hood.  this password allows the
# public key to be sent.  So its kind of like a password 
#for your password except very secure.

#after you generate you private key you must "add SSH key" on git hub.
#open your isa public key with a text editor and then copy and paste it
#

#finish set up by logging into github

sshgithub:
	ssh -T git@github.com
