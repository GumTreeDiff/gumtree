SSplotComps <-
  function(replist, subplots=1:11,
           kind="LEN", sizemethod=1, aalyear=-1, aalbin=-1, plot=TRUE, print=FALSE,
           fleets="all", fleetnames="default", sexes="all",
           datonly=FALSE, samplesizeplots=TRUE, compresidplots=TRUE, bub=FALSE,
           showsampsize=TRUE, showeffN=TRUE, minnbubble=8, pntscalar=2.6,
           pwidth=7, pheight=7, punits="in", ptsize=12, res=300,
           plotdir="default", cex.main=1, linepos=1, fitbar=FALSE, maxsize=3,
           do.sqrt=TRUE, smooth=TRUE, cohortlines=c(),
           labels = c("Length (cm)",           #1
                      "Age (yr)",              #2
                      "Year",                  #3
                      "Observed sample size",  #4
                      "Effective sample size", #5
                      "Proportion",            #6
                      "cm",                    #7
                      "Frequency",             #8
                      "Weight",                #9
                      "Length",                #10
                      "(mt)",                  #11
                      "(numbers x1000)",       #12
                      "Stdev (Age) (yr)",      #13
                      "Andre's conditional AAL plot, "), #14
           printmkt=TRUE,printsex=TRUE,
           maxrows=6,maxcols=6,maxrows2=2,maxcols2=4,rows=1,cols=1,
           fixdims=TRUE,fixdims2=FALSE,maxneff=5000,verbose=TRUE,
           scalebins=FALSE,...)
{
  ################################################################################
  # SSplotComps March 23, 2011
  ################################################################################
  if(!exists("make_multifig")) stop("you are missing the function 'make_mulitifig'")

  pngfun <- function(file,caption=NA){
    png(file=file,width=pwidth,height=pheight,
        units=punits,res=res,pointsize=ptsize)
    plotinfo <- rbind(plotinfo,data.frame(file=file,caption=caption))
    return(plotinfo)
  }
  plotinfo <- NULL

  lendbase      <- replist$lendbase
  sizedbase     <- replist$sizedbase
  agedbase      <- replist$agedbase
  condbase      <- replist$condbase
  ghostagedbase <- replist$ghostagedbase
  ghostlendbase <- replist$ghostlendbase
  ladbase       <- replist$ladbase
  wadbase       <- replist$wadbase
  tagdbase1     <- replist$tagdbase1
  tagdbase2     <- replist$tagdbase2

  nfleets       <- replist$nfleets
  nseasons      <- replist$nseasons
  seasfracs     <- replist$seasfracs
  FleetNames    <- replist$FleetNames
  nsexes        <- replist$nsexes

  titles <- NULL
  titlemkt <- ""
  if(plotdir=="default") plotdir <- replist$inputs$dir

  if(fleets[1]=="all"){
    fleets <- 1:nfleets
  }else{
    if(length(intersect(fleets,1:nfleets))!=length(fleets)){
      stop("Input 'fleets' should be 'all' or a vector of values between 1 and nfleets.")
    }
  }
  if(sexes[1]=="all") sexes <- 1:2
  if(fleetnames[1]=="default") fleetnames <- FleetNames

  # a few quantities related to data type and plot number
  if(kind=="LEN"){
    dbase_kind <- lendbase
    kindlab=labels[1]
    if(datonly){
      filenamestart <- "comp_lendat_"
      titledata <- "length comp data, "
    }else{
      filenamestart <- "comp_lenfit_"
      titledata <- "length comps, "
    }
  }
  if(kind=="GSTLEN"){
    dbase_kind <- ghostlendbase
    kindlab=labels[1]
    if(datonly){
      filenamestart <- "comp_gstlendat_"
      titledata <- "ghost length comp data, "
    }else{
      filenamestart <- "comp_gstlenfit_"
      titledata <- "ghost length comps, "
    }
  }
  if(kind=="SIZE"){
    dbase_kind <- sizedbase[sizedbase$method==sizemethod,]
    sizeunits <- unique(dbase_kind$units)
    if(length(sizeunits)>1)
      stop("!error with size units in generalized size comp plots:\n",
           "    more than one unit value per method.\n")
    if(sizeunits %in% c("in","cm"))
      kindlab <- paste(labels[10]," (",sizeunits,")",sep="")
    if(sizeunits %in% c("lb","kg"))
        kindlab <- paste(labels[9]," (",sizeunits,")",sep="")
    if(datonly){
      filenamestart <- "comp_sizedat_"
      titledata <- "size comp data, "
    }else{
      filenamestart <- "comp_sizefit_"
      titledata <- "size comps, "
    }
  }
  if(kind=="AGE"){
    dbase_kind <- agedbase
    kindlab=labels[2]
    if(datonly){
      filenamestart <- "comp_agedat_"
      titledata <- "age comp data, "
    }else{
      filenamestart <- "comp_agefit_"
      titledata <- "age comps, "
    }
  }
  if(kind=="cond"){
    dbase_kind <- condbase
    kindlab=labels[2]
    if(datonly){
      filenamestart <- "comp_condAALdat_"
      titledata <- "conditional age-at-length data, "
    }else{
      filenamestart <- "comp_condAALfit_"
      titledata <- "conditional age-at-length, "
    }
  }
  if(kind=="GSTAGE"){
    dbase_kind <- ghostagedbase
    kindlab=labels[2]
    if(datonly){
      filenamestart <- "comp_gstagedat_"
      titledata <- "ghost age comp data, "
    }else{
      filenamestart <- "comp_gstagefit_"
      titledata <- "ghost age comps, "
    }
  }
  if(kind=="GSTcond"){
    dbase_kind <- ghostagedbase
    kindlab=labels[2]
    if(datonly){
      filenamestart <- "comp_gstCAALdat_"
      titledata <- "ghost conditional age-at-length data, "
    }else{
      filenamestart <- "comp_gstCAALfit_"
      titledata <- "ghost conditional age-at-length comps, "
    }
  }
  if(kind=="L@A"){
    dbase_kind <- ladbase[ladbase$N!=0,] # remove values with 0 sample size
    kindlab=labels[2]
    filenamestart <- "comp_LAAfit_"
    titledata <- "mean length at age, "
    dbase_kind$SD <- dbase_kind$Lbin_lo/dbase_kind$N
  }
  if(kind=="W@A"){
    dbase_kind <- wadbase[wadbase$N!=0,] # remove values with 0 sample size
    kindlab=labels[2]
    filenamestart <- "comp_WAAfit_"
    titledata <- "mean weight at age, "
  }
  if(!(kind%in%c("LEN","SIZE","AGE","cond","GSTAGE","GSTLEN","L@A","W@A"))) stop("Input 'kind' to SSplotComps is not right.")

  # add asterix to indicate super periods and then remove rows labeled "skip"
  # would be better to somehow show the range of years, but that seems difficult
  # at this point
  if(any(dbase_kind$SuprPer=="Sup" & dbase_kind$Used=="skip")){
    cat("Note: removing super-period composition values labeled 'skip'\n",
        "     and designating super-period values with a '*'\n")
    dbase_kind <- dbase_kind[dbase_kind$SuprPer=="No" | dbase_kind$Used!="skip",]
    dbase_kind$YrSeasName <- paste(dbase_kind$YrSeasName,ifelse(dbase_kind$SuprPer=="Sup","*",""),sep="")
  }
  
  # loop over fleets
  for(f in fleets)
  {
    # check for the presence of data
    if(length(dbase_kind$Obs[dbase_kind$Fleet==f])>0)
    {
      dbasef <- dbase_kind[dbase_kind$Fleet==f,]
      testor    <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender==0 ])>0
      testor[2] <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3)])>0
      testor[3] <- length(dbasef$Gender[dbasef$Gender==2])>0

      # loop over genders combinations
      for(k in (1:3)[testor])
      {
        if(k==1){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender==0,]}
        if(k==2){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3),]}
        if(k==3){dbase_k <- dbasef[dbasef$Gender==2,]}
        sex <- ifelse(k==3, 2, 1)
        if(sex %in% sexes){
          # loop over partitions (discard, retain, total)
          for(j in unique(dbase_k$Part))
          {
            dbase <- dbase_k[dbase_k$Part==j,]
            # dbase is the final data.frame used in the individual plots
            # it is subset based on the kind (age, len, age-at-len), fleet, gender, and partition
  
            # check for multiple ageing error types within a year to plot separately
            max_n_ageerr <- max(apply(table(dbase$Yr,dbase$Ageerr)>0,1,sum))
            if(max_n_ageerr > 1){
              dbase$YrSeasName <- paste(dbase$YrSeasName,"a",dbase$Ageerr,sep="")
              # add fraction of season to distinguish between samples
              dbase$Yr <- dbase$Yr + (1/max_n_ageerr)*(0.5/nseasons)*dbase$Ageerr 
            }
  
            ## assemble pieces of plot title
            # sex
            if(k==1) titlesex <- "sexes combined, "
            if(k==2) titlesex <- "female, "
            if(k==3) titlesex <- "male, "
            titlesex <- ifelse(printsex,titlesex,"")
            
            # market category
            if(j==0) titlemkt <- "whole catch, "
            if(j==1) titlemkt <- "discard, "
            if(j==2) titlemkt <- "retained, "
            titlemkt <- ifelse(printmkt,titlemkt,"")
  
            # plot bars for data only or if input 'fitbar=TRUE'
            if(datonly | fitbar) bars <- TRUE else bars <- FALSE
            
            # aggregating identifiers for plot titles and filenames
            title_sexmkt <- paste(titlesex,titlemkt,sep="")
            filename_fltsexmkt <- paste("flt",f,"sex",k,"mkt",j,sep="")
            
            ### subplot 1: multi-panel composition plot
            if(1 %in% subplots & kind!="cond"){ # for age or length comps, but not conditional AAL
              ptitle <- paste(titledata,title_sexmkt, fleetnames[f],sep="") # total title
              titles <- c(ptitle,titles) # compiling list of all plot titles
              tempfun <- function(ipage,...){
                # a function to combine a bunch of repeated commands
                if(!(kind %in% c("GSTAGE","GSTLEN","L@A","W@A"))){
                  make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                                sampsize=dbase$N,effN=dbase$effN,showsampsize=showsampsize,showeffN=showeffN,
                                bars=bars,linepos=(1-datonly)*linepos,
                                nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
                                main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                                maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                }
                if(kind=="GSTAGE"){
                  make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                                sampsize=dbase$N,effN=dbase$effN,showsampsize=FALSE,showeffN=FALSE,
                                bars=bars,linepos=(1-datonly)*linepos,
                                nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
                                main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                                maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                }
                if(kind=="GSTLEN"){
                  make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                                sampsize=dbase$N,effN=dbase$effN,showsampsize=FALSE,showeffN=FALSE,
                                bars=bars,linepos=(1-datonly)*linepos,
                                nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
                                main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                                maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                }
                if(kind %in% c("L@A","W@A")){
                  make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                                ptsSD=dbase$SD,
                                sampsize=dbase$N,effN=0,showsampsize=FALSE,showeffN=FALSE,
                                nlegends=1,legtext=list(dbase$YrSeasName),
                                bars=bars,linepos=(1-datonly)*linepos,
                                main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=ifelse(kind=="W@A",labels[9],labels[1]),
                                maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                }
              } # end tempfun
              
              if(plot) tempfun(ipage=0,...)
              if(print){ # set up plotting to png file if required
                npages <- ceiling(length(unique(dbase$Yr))/maxrows/maxcols)
                for(ipage in 1:npages){
                  if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                  file <- paste(plotdir,"/",filenamestart,filename_fltsexmkt,pagetext,".png",sep="")
                  caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                  plotinfo <- pngfun(file=file, caption=caption)
                  tempfun(ipage=ipage,...)
                  dev.off()
                }
              }
            } # end subplot 1
            
            # some things related to the next two bubble plots (single or multi-panel)
            if(datonly){
              z <- dbase$Obs
              col <- rep("black",2)
              titletype <- titledata
              filetype <- "bub"
              allopen <- TRUE
            }else{
              z <- dbase$Pearson
              col <- rep("blue",2)
              titletype <- "Pearson residuals, "
              filetype <- "resids"
              allopen <- FALSE
            }
            
            ### subplot 2: single panel bubble plot for numbers at length or age
            if(2 %in% subplots & bub & kind!="cond"){
              # get growth curves if requested
              if(length(cohortlines)>0){
                growdat <- replist$endgrowth
                growdatF <- growdat[growdat$Gender==1 & growdat$Morph==min(growdat$Morph[growdat$Gender==1]),]
                if(nsexes > 1){
                  growdatM <- growdat[growdat$Gender==2 & growdat$Morph==min(growdat$Morph[growdat$Gender==2]),]
                }
              }
              ptitle <- paste(titletype, title_sexmkt, fleetnames[f],sep="")
              ptitle <- paste(ptitle," (max=",round(max(z),digits=2),")",sep="")
              titles <- c(ptitle,titles) # compiling list of all plot titles
              
              tempfun <- function(){
                bubble3(x=dbase$Yr, y=dbase$Bin, z=z, xlab=labels[3],ylab=kindlab,col=col,
                        las=1,main=ptitle,cex.main=cex.main,maxsize=pntscalar,allopen=allopen,minnbubble=minnbubble)
                # add lines for growth of individual cohorts if requested
                if(length(cohortlines)>0){
                  for(icohort in 1:length(cohortlines)){
                    cat("  Adding line for",cohortlines[icohort],"cohort\n")
                    if(k %in% c(1,2)) lines(growdatF$Age+cohortlines[icohort],growdatF$Len_Mid, col="red")  #females
                    if(k %in% c(1,3)) lines(growdatM$Age+cohortlines[icohort],growdatM$Len_Mid, col="blue") #males
                  }
                }
              }
              
              if(plot) tempfun()
              if(print){ # set up plotting to png file if required
                file <- paste(plotdir,"/",filenamestart,filetype,filename_fltsexmkt,".png",sep="")
                caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                plotinfo <- pngfun(file=file, caption=caption)
                tempfun()
                dev.off() # close device if png
              }
            } # end bubble plot
            
            ### subplot 3: multi-panel bubble plots for conditional age-at-length
            if(3 %in% subplots & kind=="cond"){
              ptitle <- paste(titletype, title_sexmkt, fleetnames[f],sep="")
              ptitle <- paste(ptitle," (max=",round(max(z),digits=2),")",sep="")
              titles <- c(ptitle,titles) # compiling list of all plot titles
              tempfun <- function(ipage,...){
                make_multifig(ptsx=dbase$Bin,ptsy=dbase$Lbin_mid,yr=dbase$Yr,size=z,
                              sampsize=dbase$N,showsampsize=showsampsize,showeffN=FALSE,
                              nlegends=1,legtext=list(dbase$YrSeasName),
                              bars=FALSE,linepos=0,main=ptitle,cex.main=cex.main,
                              xlab=labels[2],ylab=labels[1],ymin0=FALSE,maxrows=maxrows2,maxcols=maxcols2,
                              fixdims=fixdims,allopen=allopen,minnbubble=minnbubble,
                              ptscol=col[1],ptscol2=col[2],ipage=ipage,scalebins=scalebins,...)
              }
              if(plot) tempfun(ipage=0,...)
              if(print){ # set up plotting to png file if required
                npages <- ceiling(length(unique(dbase$Yr))/maxrows2/maxcols2)
                for(ipage in 1:npages){
                  if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                  file <- paste(plotdir,"/",filenamestart,filetype,filename_fltsexmkt,pagetext,".png",sep="")
                  caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                  plotinfo <- pngfun(file=file, caption=caption)
                  tempfun(ipage=ipage,...)
                  dev.off() # close device if png
                }
              }
            } # end conditional bubble plot
            ### subplots 4 and 5: multi-panel plot of point and line fit to conditional age-at-length
            #                        and Pearson residuals of A-L key for specific years
            if((4 %in% subplots | 5 %in% subplots) & aalyear[1] > 0 & kind=="cond"){
              for(y in 1:length(aalyear)){
                aalyr <- aalyear[y]
                if(length(dbase$Obs[dbase$Yr==aalyr])>0){
                  if(4 %in% subplots){
                    ### subplot 4: multi-panel plot of fit to conditional age-at-length for specific years
                    ptitle <- paste(aalyr," age-at-length bin, ",title_sexmkt,fleetnames[f],sep="")
                    titles <- c(ptitle,titles) # compiling list of all plot titles
                    ydbase <- dbase[dbase$Yr==aalyr,]
                    lenbinlegend <- paste(ydbase$Lbin_lo,labels[7],sep="")
                    lenbinlegend[ydbase$Lbin_range>0] <- paste(ydbase$Lbin_lo,"-",ydbase$Lbin_hi,labels[7],sep="")
                    tempfun <- function(ipage,...){ # temporary function to aid repeating the big function call
                      make_multifig(ptsx=ydbase$Bin,ptsy=ydbase$Obs,yr=ydbase$Lbin_lo,
                                    linesx=ydbase$Bin,linesy=ydbase$Exp,
                                    sampsize=ydbase$N,effN=ydbase$effN,showsampsize=showsampsize,showeffN=showeffN,
                                    nlegends=3,legtext=list(lenbinlegend,"sampsize","effN"),
                                    bars=FALSE,linepos=linepos,main=ptitle,cex.main=cex.main,
                                    xlab=labels[2],ylab=labels[6],maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                    fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                    }
                    if(plot) tempfun(ipage=0,...)
                    if(print){
                      npages <- ceiling(length(unique(ydbase$Yr))/maxrows/maxcols)
                      for(ipage in 1:npages){
                        if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                        file <- paste(plotdir,"/",filenamestart,filename_fltsexmkt,"_",aalyr,"_",pagetext,".png",sep="")
                        caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                        plotinfo <- pngfun(file=file, caption=caption)
                        tempfun(ipage=ipage,...)
                        dev.off() # close device if print
                      }
                    }
                  } # end if 4 in subplots
                  if(5 %in% subplots){
                    ### subplot 5: Pearson residuals for A-L key
                    z <- ydbase$Pearson
                    ptitle <- paste(aalyr," Pearson residuals for A-L key, ",title_sexmkt,fleetnames[f],sep="")
                    ptitle <- paste(ptitle," (max=",round(abs(max(z)),digits=2),")",sep="")
                    titles <- c(ptitle,titles) # compiling list of all plot titles
                    tempfun <- function(){
                      bubble3(x=ydbase$Bin,y=ydbase$Lbin_lo,z=z,xlab=labels[2],ylab=labels[1],col=rep("blue",2),
                              las=1,main=ptitle,cex.main=cex.main,maxsize=pntscalar,allopen=FALSE,minnbubble=minnbubble)
                    }
                    if(plot) tempfun()
                    if(print){
                      file <- paste(plotdir,"/",filenamestart,"yearresids_",filename_fltsexmkt,"_",aalyr,".png",sep="")
                      caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                      plotinfo <- pngfun(file=file, caption=caption)
                      tempfun()
                      dev.off() # close device if print
                    }
                  } # end if 5 in subplots
                }
              }
            }
            
            ### subplot 6: multi-panel plot of point and line fit to conditional age-at-length
            #                   for specific length bins
            if(6 %in% subplots & aalbin[1] > 0){
              badbins <- setdiff(aalbin, dbase$Lbin_hi)
              goodbins <- intersect(aalbin, dbase$Lbin_hi)
              if(length(goodbins)>0){
                if(length(badbins)>0){
                  cat("Error! the following inputs for 'aalbin' do not match the Lbin_hi values for the conditional age-at-length data:",badbins,"\n",
                      "       the following inputs for 'aalbin' are fine:",goodbins,"\n")
                }
                for(ibin in 1:length(goodbins)){ # loop over good bins
                  ilenbin <- goodbins[ibin]
                  abindbase <- dbase[dbase$Lbin_hi==ilenbin,]
                  if(nrow(abindbase)>0){ # check for data associated with this bin
                    ptitle <- paste("Age-at-length ",ilenbin,labels[7],", ",title_sexmkt,fleetnames[f],sep="")
                    titles <- c(ptitle,titles) # compiling list of all plot titles
                    tempfun <- function(ipage,...){ # temporary function to aid repeating the big function call
                      make_multifig(ptsx=abindbase$Bin,ptsy=abindbase$Obs,yr=abindbase$Yr,linesx=abindbase$Bin,linesy=abindbase$Exp,
                                    sampsize=abindbase$N,effN=abindbase$effN,showsampsize=showsampsize,showeffN=showeffN,
                                    nlegends=3,legtext=list(abindbase$YrSeasName,"sampsize","effN"),
                                    bars=bars,linepos=(1-datonly)*linepos,
                                    main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                    fixdims=fixdims,ipage=ipage,scalebins=scalebins,...)
                    }
                    if(plot) tempfun(ipage=0,...)
                    if(print){
                      npages <- ceiling(length(unique(abindbase$Yr))/maxrows/maxcols)
                      for(ipage in 1:npages){
                        if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                        file <- paste(plotdir,filenamestart,filename_fltsexmkt,"_length",ilenbin,labels[7],pagetext,".png",sep="")
                        caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                        plotinfo <- pngfun(file=file, caption=caption)
                        tempfun(ipage=ipage,...)
                        dev.off() # close device if print
                      }
                    } # end print
                  } # end if data
                } # end loop over length bins
              } # end if length(goodbins)>0
            } # end if plot requested
            
            ### subplot 7: sample size plot
            if(7 %in% subplots & samplesizeplots & !datonly & !(kind %in% c("GSTAGE","GSTLEN","L@A","W@A"))){
              ptitle <- paste("N-EffN comparison, ",titledata,title_sexmkt,fleetnames[f], sep="")
              titles <- c(ptitle,titles) # compiling list of all plot titles
              lfitfunc <- function(){
                if(kind=="cond"){
                  # trap nonrobust effective n's
                  # should this only be for conditional age-at-length or all plots?
                  dbasegood <- dbase[dbase$Obs>=0.0001 & dbase$Exp<0.99 & !is.na(dbase$effN) & dbase$effN<maxneff,]
                }else{
                  dbasegood <- dbase
                }
                if(nrow(dbasegood)>0){
                  plot(dbasegood$N,dbasegood$effN,xlab=labels[4],main=ptitle,cex.main=cex.main,
                       ylim=c(0,1.05*max(dbasegood$effN)),xlim=c(0,1.05*max(dbasegood$N)),
                       col="blue",pch=19,ylab=labels[5],xaxs="i",yaxs="i")
                  abline(h=0,col="grey")
                  abline(0,1,col="black")
                  # add loess smoother if there's at least 6 points with a range greater than 2
                  if(smooth & length(unique(dbasegood$N)) > 6 & diff(range(dbasegood$N))>2){
                    psmooth <- loess(dbasegood$effN~dbasegood$N,degree=1)
                    lines(psmooth$x[order(psmooth$x)],psmooth$fit[order(psmooth$x)],lwd=1.2,col="red",lty="dashed")
                  }
                }
              }
              if(plot) lfitfunc()
              if(print){ # set up plotting to png file if required
                file <- paste(plotdir,filenamestart,"sampsize_",filename_fltsexmkt,".png",sep="")
                caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                plotinfo <- pngfun(file=file, caption=caption)
                lfitfunc()
                dev.off()
              }
            } # end subplot 7
    
            ### subplot 8: Andre's mean age and std. dev. in conditional AAL
            if(8 %in% subplots & kind=="cond"){
              ptitle <- paste(labels[14], title_sexmkt, fleetnames[f],sep="")
              andrefun <- function(ipage=0){
                Lens <-sort(unique(dbase$Lbin_lo))
                Yrs <- sort(unique(dbase$Yr))

                # do some stuff so that figures that span multiple pages can be output as separate PNG files
                npanels <- length(Yrs)
                andrerows <- 3
                npages <- npanels/andrerows
                panelrange <- 1:npanels
                if(npages > 1 & ipage!=0) panelrange <- intersect(panelrange, 1:andrerows + andrerows*(ipage-1))
                Yrs2 <- Yrs[panelrange]

                par(mfrow=c(andrerows,2),mar=c(2,4,1,1),oma=c(3,0,3,0))
                for (Yr in Yrs2){
                  y <- dbase[dbase$Yr==Yr,]
                  Size <- NULL; Size2 <- NULL
                  Obs <- NULL; Obs2 <- NULL
                  Pred <- NULL;  Pred2 <- NULL
                  Upp <- NULL; Low <- NULL; Upp2 <- NULL; Low2 <- NULL
                  for (Ilen in Lens){
                    z <- y[y$Lbin_lo == Ilen,]
                    if (length(z[,1]) > 0){
                      weightsPred <- z$Exp/sum(z$Exp)
                      weightsObs <- z$Obs/sum(z$Obs)
                      ObsV <- sum(z$Bin*weightsObs)
                      ObsV2 <- sum(z$Bin*z$Bin*weightsObs)
                      PredV <- sum(z$Bin*weightsPred)
                      PredV2 <- sum(z$Bin*z$Bin*weightsPred)
                      # Overdispersion on N
                      # NN <- z$N[1]*0.01 # Andre did this for reasons unknown
                      NN <- z$N[1]
                      if (max(z$Obs) > 1.0e-4){
                        Size <- c(Size,Ilen)
                        Obs <- c(Obs,ObsV)
                        Pred <- c(Pred,PredV)
                        varn <-sqrt(PredV2-PredV*PredV)/sqrt(NN)
                        Pred2 <- c(Pred2,varn)
                        varn <-sqrt(max(0,ObsV2-ObsV*ObsV))/sqrt(NN)
                        Obs2 <- c(Obs2,varn)
                        Low <- c(Low,ObsV-1.64*varn)
                        Upp <- c(Upp,ObsV+1.64*varn)
                        if (NN > 1){
                          Size2 <- c(Size2,Ilen)
                          Low2 <- c(Low2,varn*sqrt((NN-1)/qchisq(0.95,NN)))
                          Upp2 <- c(Upp2,varn*sqrt((NN-1)/qchisq(0.05,NN)))
                        }
                      }
                    }
                  }
                  if (length(Obs) > 0){
                    ymax <- max(Pred,Obs,Upp)*1.1
                    plot(Size,Obs,xlab="",ylab="Age",pch=16,xlim=c(min(Lens),max(Lens)),ylim=c(0,ymax),yaxs="i")
                    text(x=par("usr")[1],y=.9*ymax,labels=Yr,adj=c(-.5,0),font=2,cex=1.2)
                    lines(Size,Pred)
                    lines(Size,Low,lty=3)
                    lines(Size,Upp,lty=3)
                    #title(paste("Year = ",Yr,"; Gender = ",Gender))
                    
                    if(par("mfg")[1] & par("mfg")[2]==1){ # first plot on any new page
                      title(main=ptitle,xlab=labels[1],outer=TRUE,line=1)
                    }
                    ymax <- max(Obs2,Pred2)*1.1
                    plot(Size,Obs2,xlab=labels[1],ylab=labels[13],pch=16,xlim=c(min(Lens),max(Lens)),ylim=c(0,ymax),yaxs="i")
                    lines(Size,Pred2)
                    lines(Size2,Low2,lty=3)
                    lines(Size2,Upp2,lty=3)

                    
                  } # end if data exist
                } # end loop over years
              } # end andrefun
              if(plot) andrefun()
              if(print){ # set up plotting to png file if required
                npages <- ceiling(length(unique(dbase$Yr))/3)
                for(ipage in 1:npages){
                  if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                  file <- paste(plotdir,"/",filenamestart,"Andre_plots",filename_fltsexmkt,pagetext,".png",sep="")
                  caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                  plotinfo <- pngfun(file=file, caption=caption)
                  andrefun(ipage=ipage)
                  dev.off() # close device if png
                } # end loop over pages
              } # end test for print to PNG option
            } # end subplot 8
          } # end loop over partitions
        } # end test for whether gender in vector of requested sexes
      } # end loop over combined/not-combined genders
    } # end if data
  } # end loop over fleets

  ### subplot 9: by fleet aggregating across years
  if(9 %in% subplots & kind!="cond") # for age or length comps, but not conditional AAL
  {
    dbasef <- dbase_kind[dbase_kind$Fleet %in% fleets,]
    # check for the presence of data
    if(nrow(dbasef)>0)
    {
      testor    <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender==0 ])>0
      testor[2] <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3)])>0
      testor[3] <- length(dbasef$Gender[dbasef$Gender==2])>0

      # loop over genders combinations
      for(k in (1:3)[testor])
      {
        if(k==1){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender==0,]}
        if(k==2){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3),]}
        if(k==3){dbase_k <- dbasef[dbasef$Gender==2,]}
        sex <- ifelse(k==3, 2, 1)

        # loop over partitions (discard, retain, total)
        for(j in unique(dbase_k$Part))
        {
          # dbase is the final data.frame used in the individual plots
          # it is subset based on the kind (age, len, age-at-len), fleet, gender, and partition
          dbase <- dbase_k[dbase_k$Part==j,]
          if(nrow(dbase)>0){
            ## assemble pieces of plot title
            # sex
            if(k==1) titlesex <- "sexes combined, "
            if(k==2) titlesex <- "female, "
            if(k==3) titlesex <- "male, "
            titlesex <- ifelse(printsex,titlesex,"")
  
            # market category
            if(j==0) titlemkt <- "whole catch, "
            if(j==1) titlemkt <- "discard, "
            if(j==2) titlemkt <- "retained, "
            titlemkt <- ifelse(printmkt,titlemkt,"")
  
            # plot bars for data only or if input 'fitbar=TRUE'
            if(datonly | fitbar) bars <- TRUE else bars <- FALSE
  
            # aggregating identifiers for plot titles and filenames
            title_sexmkt <- paste(titlesex,titlemkt,sep="")
            filename_fltsexmkt <- paste("sex",k,"mkt",j,sep="")
  
            ptitle <- paste(titledata,title_sexmkt, "aggregated across time by fleet",sep="") # total title
            titles <- c(ptitle,titles) # compiling list of all plot titles
  
            Bins <- sort(unique(dbase$Bin))
            nbins <- length(Bins)
            df <- data.frame(N=dbase$N,
                             effN=dbase$effN,
                             obs=dbase$Obs*dbase$N,
                             exp=dbase$Exp*dbase$N)
            agg <- aggregate(x=df, by=list(bin=dbase$Bin,f=dbase$Fleet), FUN=sum)
            agg <- agg[agg$f %in% fleets,]
            agg$obs <- agg$obs/agg$N
            agg$exp <- agg$exp/agg$N
            # note: sample sizes will be different for each bin if tail compression is used
            #       printed sample sizes in plot will be maximum, which may or may not
            #       represent sum of sample sizes over all years/ages
            for(f in unique(agg$f)){
              infleet <- agg$f==f
              agg$N[infleet] <- max(agg$N[infleet])
              agg$effN[infleet] <- max(agg$effN[infleet])
            }

            namesvec <- fleetnames[agg$f]
            if(!(kind %in% c("GSTAGE","GSTLEN","L@A","W@A"))){
              # group remaining calculations as a function
              tempfun <- function(ipage,...){
                make_multifig(ptsx=agg$bin,ptsy=agg$obs,yr=agg$f,
                              linesx=agg$bin,linesy=agg$exp,
                              sampsize=agg$N,effN=agg$effN,
                              showsampsize=showsampsize,showeffN=showeffN,
                              bars=bars,linepos=(1-datonly)*linepos,
                              nlegends=3,
                              legtext=list(namesvec,"sampsize","effN"),
                              main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                              maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                              fixdims=fixdims2,ipage=ipage,lwd=2,scalebins=scalebins,...)
              }
              if(plot) tempfun(ipage=0,...) 
              if(print){ # set up plotting to png file if required
                npages <- ceiling(length(unique(agg$f))/maxrows/maxcols)
                for(ipage in 1:npages){
                  if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                  file <- paste(plotdir,filenamestart,filename_fltsexmkt,pagetext,"aggregated across time.png",sep="")
                  caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                  plotinfo <- pngfun(file=file, caption=caption)
                  tempfun(ipage=ipage,...)
                  dev.off()
                }
              } # end print function
            }else{
            # haven't configured this aggregated plot for other types
                ## if(kind=="GSTAGE"){
                ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                ##                 sampsize=dbase$N,effN=dbase$effN,showsampsize=FALSE,showeffN=FALSE,
                ##                 bars=bars,linepos=(1-datonly)*linepos,
                ##                 nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
                ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                ##                 fixdims=fixdims,ipage=ipage,...)
                ## }
                ## if(kind %in% c("L@A","W@A")){
                ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                ##                 sampsize=dbase$N,effN=0,showsampsize=FALSE,showeffN=FALSE,
                ##                 nlegends=1,legtext=list(dbase$YrSeasName),
                ##                 bars=bars,linepos=(1-datonly)*linepos,
                ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=ifelse(kind=="W@A",labels[9],labels[1]),
                ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                ##                 fixdims=fixdims,ipage=ipage,...)
                ## }
            }
          } # end test for presence of observations in this partition
        } # end loop over partitions
      } # end loop over combined/not-combined genders
    } # end if data
  } # end subplot 9

  ### subplot 10: by fleet aggregating across years
  if(10 %in% subplots & kind!="cond" & nseasons>1) # for age or length comps, but not conditional AAL
  {
    dbasef <- dbase_kind[dbase_kind$Fleet %in% fleets,]
    # check for the presence of data
    if(nrow(dbasef)>0)
    {
      testor    <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender==0 ])>0
      testor[2] <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3)])>0
      testor[3] <- length(dbasef$Gender[dbasef$Gender==2])>0

      # loop over genders combinations
      for(k in (1:3)[testor])
      {
        if(k==1){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender==0,]}
        if(k==2){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3),]}
        if(k==3){dbase_k <- dbasef[dbasef$Gender==2,]}
        sex <- ifelse(k==3, 2, 1)

        # loop over partitions (discard, retain, total)
        for(j in unique(dbase_k$Part))
        {
          # dbase is the final data.frame used in the individual plots
          # it is subset based on the kind (age, len, age-at-len), fleet, gender, and partition
          dbase <- dbase_k[dbase_k$Part==j,]
          if(nrow(dbase)>0){
            ## assemble pieces of plot title
            # sex
            if(k==1) titlesex <- "sexes combined, "
            if(k==2) titlesex <- "female, "
            if(k==3) titlesex <- "male, "
            titlesex <- ifelse(printsex,titlesex,"")
  
            # market category
            if(j==0) titlemkt <- "whole catch, "
            if(j==1) titlemkt <- "discard, "
            if(j==2) titlemkt <- "retained, "
            titlemkt <- ifelse(printmkt,titlemkt,"")
  
            # plot bars for data only or if input 'fitbar=TRUE'
            if(datonly | fitbar) bars <- TRUE else bars <- FALSE
  
            # aggregating identifiers for plot titles and filenames
            title_sexmkt <- paste(titlesex,titlemkt,sep="")
            filename_fltsexmkt <- paste("sex",k,"mkt",j,sep="")
  
            ptitle <- paste(titledata,title_sexmkt, "\naggregated within season by fleet",sep="") # total title
            titles <- c(ptitle,titles) # compiling list of all plot titles
  
            Bins <- sort(unique(dbase$Bin))
            nbins <- length(Bins)
            df <- data.frame(N=dbase$N,
                             effN=dbase$effN,
                             obs=dbase$Obs*dbase$N,
                             exp=dbase$Exp*dbase$N)
            agg <- aggregate(x=df, by=list(bin=dbase$Bin,f=dbase$Fleet,s=dbase$Seas), FUN=sum)
            agg <- agg[agg$f %in% fleets,]
            if(any(agg$s<=0)){
              cat("super-periods may not work correctly in plots of aggregated comps\n")
              agg <- agg[agg$s > 0,]
            }
            agg$obs <- agg$obs/agg$N
            agg$exp <- agg$exp/agg$N
            # note: sample sizes will be different for each bin if tail compression is used
            #       printed sample sizes in plot will be maximum, which may or may not
            #       represent sum of sample sizes over all years/ages
            for(f in unique(agg$f)){
              for(s in unique(agg$s[agg$ff==f])){
                infleetseas <- agg$f==f & agg$s==s
                agg$N[infleetseas] <- max(agg$N[infleetseas])
                agg$effN[infleetseas] <- max(agg$effN[infleetseas])
              }
            }
            agg$fseas <- agg$f + seasfracs[agg$s]

            namesvec <- paste(fleetnames[agg$f]," s",agg$s,sep="")

            # group remaining calculations as a function
            tempfun <- function(ipage,...){
              if(!(kind %in% c("GSTAGE","GSTLEN","L@A","W@A"))){
                make_multifig(ptsx=agg$bin,ptsy=agg$obs,yr=agg$fseas,
                              linesx=agg$bin,linesy=agg$exp,
                              sampsize=agg$N,effN=agg$effN,
                              showsampsize=showsampsize,showeffN=showeffN,
                              bars=bars,linepos=(1-datonly)*linepos,
                              nlegends=3,
                              legtext=list(namesvec,"sampsize","effN"),
                              main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                              maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                              fixdims=fixdims2,ipage=ipage,lwd=2,scalebins=scalebins,...)
              }
  
         # haven't configured this aggregated plot for other types
              ## if(kind=="GSTAGE"){
              ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
              ##                 sampsize=dbase$N,effN=dbase$effN,showsampsize=FALSE,showeffN=FALSE,
              ##                 bars=bars,linepos=(1-datonly)*linepos,
              ##                 nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
              ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
              ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
              ##                 fixdims=fixdims,ipage=ipage,...)
              ## }
              ## if(kind %in% c("L@A","W@A")){
              ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
              ##                 sampsize=dbase$N,effN=0,showsampsize=FALSE,showeffN=FALSE,
              ##                 nlegends=1,legtext=list(dbase$YrSeasName),
              ##                 bars=bars,linepos=(1-datonly)*linepos,
              ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=ifelse(kind=="W@A",labels[9],labels[1]),
              ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
              ##                 fixdims=fixdims,ipage=ipage,...)
              ## }
  
            }
            if(plot) tempfun(ipage=0,...) 
            if(print){ # set up plotting to png file if required
              npages <- ceiling(length(unique(agg$fseas))/maxrows/maxcols)
              for(ipage in 1:npages)
              {
                if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                file <- paste(plotdir,filenamestart,filename_fltsexmkt,pagetext,
                              "aggregated within season.png",sep="")
                caption <- paste(ptitle, " (plot ",ipage,"of ",npages,")",sep="")
                plotinfo <- pngfun(file=file, caption=caption)
                tempfun(ipage=ipage,...)
                dev.off()
              }
            } # end print function
          } # end test for presence of observations in this partition
        } # end loop over partitions
      } # end loop over combined/not-combined genders
    } # end if data
  } # end subplot 10
  
  ### subplot 11: by fleet aggregating across years
  if(11 %in% subplots & kind!="cond" & nseasons>1){ # for age or length comps, but not conditional AAL
    # loop over fleets
    for(f in fleets){
      dbasef <- dbase_kind[dbase_kind$Fleet==f,]
      # check for the presence of data
      if(nrow(dbasef)>0){
        testor    <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender==0 ])>0
        testor[2] <- length(dbasef$Gender[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3)])>0
        testor[3] <- length(dbasef$Gender[dbasef$Gender==2])>0
        # loop over genders combinations
        for(k in (1:3)[testor]){
          if(k==1){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender==0,]}
          if(k==2){dbase_k <- dbasef[dbasef$Gender==1 & dbasef$Pick_gender %in% c(1,3),]}
          if(k==3){dbase_k <- dbasef[dbasef$Gender==2,]}
          sex <- ifelse(k==3, 2, 1)

          # loop over partitions (discard, retain, total)
          for(j in unique(dbase_k$Part)){
            # dbase is the final data.frame used in the individual plots
            # it is subset based on the kind (age, len, age-at-len), fleet, gender, and partition
            dbase <- dbase_k[dbase_k$Part==j,]
            if(nrow(dbase)>0){
              ## assemble pieces of plot title
              # sex
              if(k==1) titlesex <- "sexes combined, "
              if(k==2) titlesex <- "female, "
              if(k==3) titlesex <- "male, "
              titlesex <- ifelse(printsex,titlesex,"")
              
              # market category
              if(j==0) titlemkt <- "whole catch, "
              if(j==1) titlemkt <- "discard, "
              if(j==2) titlemkt <- "retained, "
              titlemkt <- ifelse(printmkt,titlemkt,"")
              
              # plot bars for data only or if input 'fitbar=TRUE'
              if(datonly | fitbar) bars <- TRUE else bars <- FALSE
              
              # aggregating identifiers for plot titles and filenames
              title_sexmkt <- paste(titlesex,titlemkt,sep="")
              filename_fltsexmkt <- paste("flt",f,"sex",k,"mkt",j,sep="")

              Bins <- sort(unique(dbase$Bin))
              nbins <- length(Bins)
              df <- data.frame(N=dbase$N,
                               effN=dbase$effN,
                               obs=dbase$Obs*dbase$N,
                               exp=dbase$Exp*dbase$N)
              agg <- aggregate(x=df, by=list(bin=dbase$Bin,f=dbase$Fleet,y=floor(dbase$Yr)), FUN=sum)
              agg <- agg[agg$f %in% fleets,]
              agg$obs <- agg$obs/agg$N
              agg$exp <- agg$exp/agg$N
              # note: sample sizes will be different for each bin if tail compression is used
              #       printed sample sizes in plot will be maximum, which may or may not
              #       represent sum of sample sizes over all years/ages
              for(f in unique(agg$f)){
                for(y in unique(agg$y[agg$ff==f])){
                  infleetyr <- agg$f==f & agg$y==y
                  agg$N[infleetyr] <- max(agg$N[infleetyr])
                  agg$effN[infleetyr] <- max(agg$effN[infleetyr])
                }
              }
              agg$fy <- agg$f + agg$y/10000

              # group remaining calculations as a function
              tempfun <- function(ipage,...){
                ptitle <- paste(titledata,title_sexmkt,fleetnames[f], "\naggregated across seasons within year",sep="") # total title
                if(!(kind %in% c("GSTAGE","GSTLEN","L@A","W@A"))){
                  make_multifig(ptsx=agg$bin,ptsy=agg$obs,yr=agg$fy,
                                linesx=agg$bin,linesy=agg$exp,
                                sampsize=agg$N,effN=agg$effN,
                                showsampsize=showsampsize,showeffN=showeffN,
                                bars=bars,linepos=(1-datonly)*linepos,
                                nlegends=3,
                                legtext=list(agg$y,"sampsize","effN"),
                                main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                                maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                                fixdims=fixdims2,ipage=ipage,lwd=2,scalebins=scalebins,...)
                }
                
                # haven't configured this aggregated plot for other types
                ## if(kind=="GSTAGE"){
                ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                ##                 sampsize=dbase$N,effN=dbase$effN,showsampsize=FALSE,showeffN=FALSE,
                ##                 bars=bars,linepos=(1-datonly)*linepos,
                ##                 nlegends=3,legtext=list(dbase$YrSeasName,"sampsize","effN"),
                ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=labels[6],
                ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                ##                 fixdims=fixdims,ipage=ipage,...)
                ## }
                ## if(kind %in% c("L@A","W@A")){
                ##   make_multifig(ptsx=dbase$Bin,ptsy=dbase$Obs,yr=dbase$Yr,linesx=dbase$Bin,linesy=dbase$Exp,
                ##                 sampsize=dbase$N,effN=0,showsampsize=FALSE,showeffN=FALSE,
                ##                 nlegends=1,legtext=list(dbase$YrSeasName),
                ##                 bars=bars,linepos=(1-datonly)*linepos,
                ##                 main=ptitle,cex.main=cex.main,xlab=kindlab,ylab=ifelse(kind=="W@A",labels[9],labels[1]),
                ##                 maxrows=maxrows,maxcols=maxcols,rows=rows,cols=cols,
                ##                 fixdims=fixdims,ipage=ipage,...)
                ## }
                
              } # end tempfun

              if(plot) tempfun(ipage=0,...) 
              if(print){ # set up plotting to png file if required
                npages <- ceiling(length(unique(agg$fy))/maxrows/maxcols)
                for(ipage in 1:npages){
                  if(npages>1) pagetext <- paste("_page",ipage,sep="") else pagetext <- ""
                  filename <- paste(plotdir,filenamestart,filename_fltsexmkt,pagetext,
                                    "aggregated across seasons within year.png",sep="")
                  pngfun(file=filename)
                  tempfun(ipage=ipage,...)
                  dev.off()
                }
              } # end print function
            } # end test for presence of observations in this partition
          } # end loop over partitions
        } # end loop over combined/not-combined genders
      } # end if data
    } # end loop over fleets
  } # end subplot 11
  if(!is.null(plotinfo)) plotinfo$category <- "Comp"
  return(invisible(plotinfo))
} # end embedded SSplotComps function
###########################
