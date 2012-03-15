# the purpose of this make file is to make
# pushing and pulling easier
NAME =Derrick Ho
USERNAME =wh1pch81n
EMAIL = wh1pch81n@gmail.com
REPOSITORY=nachos
COMMENT = add comments here

add:
	git add ./nachos
commit:
	git commit -m '$(COMMENT)'
push:
	git push -u origin master
pull:
	git pull

#below commands used for set up.  Should only do this once
configname:
	git config --global user.name "$(NAME)"
configemail:
	git config --global user.email $(EMAIL)
init:
	git init
remote:
	git remote add origin git@github.com:$(USERNAME)/$(REPOSITORY).git