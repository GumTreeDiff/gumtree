TSCplot <- function(SSout,
                    yrs="default",
                    ylimBar="default",
                    ylimDepl=c(0,1.025),
                    colBar= "yellow",
                    cexBarLabels=1.1,
                    cex.axis=1.1,
                    space=0.0,
                    pchDepl=19,
                    colDepl="red",
                    lwdDepl=3,
                    shiftDepl = 0.25,
                    pchSpace = 5,
                    ht=4,wd=7,
                    labelLines=2.8,
                    makePDF=NULL,
                    makePNG=NULL,
                    MCMC=F)  {
                    
    ### Plots the barchart of catches and depletion trajctory for the TSC report

    if(!is.null(makePDF) & !is.null(makePNG)) stop("Cannot specify both makePDF and makePNG. Choose only one.\n")

    indVirgin <- which(SSout$timeseries$Era=="VIRG")
    ind <- which(SSout$timeseries$Era=="TIME")
    ind <- c(ind,max(ind)+1)
    if(yrs[1]=="default") yrs <- unique(sort(SSout$timeseries$Yr[ind]))
    
    deadCatch <- SSplotCatch(SSout,plot=F,verbose=F)$totcatchmat     #get catches + discards summed over areas
    if(ncol(deadCatch) > 2) {  #sum over fisheries
        deadCatch <- cbind(apply(deadCatch[,-ncol(deadCatch)],1,sum),deadCatch[,ncol(deadCatch)])
    }
    deadCatch <- deadCatch[match(yrs,deadCatch[,2]),]
    rownames(deadCatch) <- yrs

    if(!MCMC) {
        SBzero <- SSout$SBzero
        SB <- SSout$derived_quants[substring(SSout$derived_quants$LABEL,1,4)=="SPB_",]
        SB <- SB[match(as.character(yrs),substring(SB$LABEL,5)),]
        depl <- SSout$derived_quants[substring(SSout$derived_quants$LABEL,1,7)=="Bratio_",]
        depl <- depl[match(as.character(yrs),substring(depl$LABEL,8)),]
        SP <- data.frame(Yr=yrs, SpawnBio=SB[,"Value"], Depl=depl[,"Value"],Dead_Catch=deadCatch[,1])
    }
    if(MCMC) {
        if(is.null(SSout$mcmc)) stop("There is no mcmc element on the model list.\nSet MCMC=F or add in the mcmc element to the list.\n")
        SBzero <- median(SSout$mcmc$SPB_Virgin)
        SB <- SSout$mcmc[,substring(names(SSout$mcmc),1,4)=="SPB_"]
        SB <- apply(SB[,match(as.character(yrs),substring(names(SB),5))],2,median)
        depl <- SSout$mcmc[,substring(names(SSout$mcmc),1,7)=="Bratio_"]
        tmp1 <- match(as.character(yrs),substring(names(depl),8))  #can have an NA in it and will cause an error
        tmp2 <- tmp1[!is.na(tmp1)]   #remove NA's to get the medians
        depl <- apply(depl[,tmp2],2,median)
        depl <- depl[match(as.character(yrs),substring(names(depl),8))]
        SP <- data.frame(Yr=yrs, SpawnBio=SB, Depl=depl, Dead_Catch=deadCatch[,1])
    }
    
    if(ylimBar=="default") {
        ylimBar <- c(0,max(SP$Dead_Catch,na.rm=T)*1.05)
    }
    ind <- seq(1,nrow(SP),pchSpace)
    
    if(is.null(makePDF)) { windows(height=ht,width=wd) }
    if(!is.null(makePDF)) { pdf(file=makePDF,width=wd,height=ht) }
    if(!is.null(makePNG)) { png(file=makePNG,width=wd,height=ht,units = "in", pointsize = 10, res=300) }
    par(mar=c(4,5,2,5))
    barOut <- barplot(SP$Dead_Catch,  names.arg = SP$Yr, ylim=ylimBar, ylab="", col='yellow', cex=cexBarLabels, cex.axis=cex.axis, space=space,xlim=c(0,nrow(SP)),axisnames=F)
    axis(1,at=barOut[ind,1],labels=yrs[ind])
    par(new=T)
    xpts <- (0:(nrow(SP)-1))+shiftDepl
    plot(xpts, SP$Depl, yaxt='n', yaxs='i', xaxt = 'n', ylab="", xlab="",
           ylim=ylimDepl, type='l', lwd=lwdDepl,  cex.axis=cex.axis, xlim=c(0,nrow(SP)))
    points(xpts[ind], SP$Depl[ind], pch=pchDepl, col=colDepl)
    axis(4, at=seq(ylimDepl[1], ylimDepl[2], 0.1), cex.axis=cex.axis)
    mtext(c("Year","Total mortality catch (mt)", "Depletion"), side=c(1,2,4), line=labelLines, cex=1.5)

    if(!is.null(makePDF)) {
        dev.off()
        cat("The plot is in pdf file",makePDF,"\n")
    }
    if(!is.null(makePNG)) {
        dev.off()
        cat("The plot is in png file",makePNG,"\n")
    }

    invisible(SP)
}
