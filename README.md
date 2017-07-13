# receipt-workflow
Spring boot application for automatically moving pdf file from Google drive to Shoeboxed, and then from shoeboxed to Dropbox

This application was (and still is, in this state) a very crude one for filling a specific need : 

Being a freelancer, I have a lot of receipt to handle.

Being something deorganized myself, I needed a way to organize them.

Being a lazy one, I needed a way to do this (and sending them to my accountant) without bothering myself too much.

Here is the workflow I came to : 
 1. Using the google Drive Smartphone app to scan the receipt and put them in a specific folder
 2. Moving these files from Drive to Shoeboxed, a webapp providing OCR for receipts
 3. Once a day, in Shoeboxed, check the moved files, correct the information and mark them for transmission to the accountant
 4. Copy the file to the accountant's dropbox, rename them with Shoeboxed information (easier for the accountant) and sent the accountant an email with the files list (just to be sure)
 5. unmark the file in Shoeboxed
 

My goald here is to automatize most of these steps.

 - Step 1 cannot be automatize, of course. And it is needed because the Smartphone app of Shoeboxed is a crappy one (no cropping)
 - Step 2 is automatized right now
 - Step 3 cannot be automatized
 - Step 4 is done 'on demand", using the browser. Needed because the Shoeboxed API is a crappy one, and don't allow me to unmark automatically a file
 - Step 5 needed. See Step 4 explanation for why. 
 
I need to add some additionnal features to allow a colleague to use the application. See the issues page for information about that. Once it is done, I will think about the feature of this application. 

I will get rid of shoeboxed (too many crap. I was thinking about replacing it with datamolino), that's a given, but I don't know if I will update this application or rewrite it using apache-camel. Feel free to drop a word if you have an opinion about that.



 
 
  

