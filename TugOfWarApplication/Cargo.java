/** This class defines a cargo pulled by several plus and minus motors engaged in a tug-of-war.
 *  It is to be used in the tug-of-war animation TugOfWarAnimation.java
 *  Written by Melanie J.I. Mueller, Max Planck Institute of Colloids and Interfaces, August 2008
 */

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.text.*;

class Cargo{

DecimalFormat doubleFormat1 = new DecimalFormat("0.0"); // formatting for output of doubles
DecimalFormat doubleFormat2 = new DecimalFormat("0.00"); // formatting for output of doubles
DecimalFormat doubleFormat3 = new DecimalFormat("0.000"); // formatting for output of doubles

final double l=0.08;
final double filamentLength=10.;		// filament length [mum], corresponds to L [pixel]

// motor parameter default values
private final int NpStart=4,NmStart=4;
private final double FsPStart=6.,FdPStart=3.,epsPStart=1.,piPStart=5.,vFPStart=1.,vBPStart=0.006;
private final double FsMStart=6.,FdMStart=3.,epsMStart=1.,piMStart=5.,vFMStart=1.,vBMStart=0.006;
private final double tStart=0.,dtStart=0.01,xStart=0.;
// motor parameters variables
private int Np,Nm,np,nm;
private double FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM;
private double offProbaPlus[][],offProbaMinus[][],onProbaPlus[][],onProbaMinus[][];

// cargo parameters
private double x,t,dt,xAbs;

private double randPM,rand,prob,sum;
private boolean changedMotorNumber;


// Constructors
public Cargo(){
	resetCargoParas();
}
// public Cargo(int Np,int Nm,
// 	double FsP,double FdP,double epsP,double piP,double vFP, double vBP,
// 	double FsM,double FdM,double epsM,double piM,double vFM, double vBM,
// 	double x,double t,double dt){
// 	setCargoParas(Np,Nm,FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM,x,t,dt);
// }
public Cargo(int Np,int Nm,
	double FsP,double FdP,double epsP,double piP,double vFP, double vBP,
	double FsM,double FdM,double epsM,double piM,double vFM, double vBM){
	setCargoParas(Np,Nm,FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM,xStart,tStart,dtStart);
}

public final void resetCargoParas(){
	setCargoParas(NpStart,NmStart,FsPStart,FdPStart,epsPStart,piPStart,vFPStart,vBPStart,
							FsMStart,FdMStart,epsMStart,piMStart,vFMStart,vBMStart,xStart,tStart,dtStart);
}

final public double getdt(){return this.dt;}
final public double getVelocity(){return this.velocity(np,nm);}
final public double getPosition(){return this.x;}
final public double getDistanceTravelled(){return this.xAbs;}
final public double getTime(){return this.t;}
final public double getFp(){return plusMotorForce(np,nm);}
final public double getFm(){return minusMotorForce(np,nm);}
final public int getNp(){return this.Np;}
final public int getNm(){return this.Nm;}
final public int getnp(){return this.np;}
final public int getnm(){return this.nm;}
final public double getFsP(){return this.FsP;}final public double getFdP(){return this.FdP;}
final public double getepsP(){return this.epsP;}final public double getpiP(){return this.piP;}
final public double getvFP(){return this.vFP;}final public double getvBP(){return this.vBP;}
final public double getFsM(){return this.FsM;}final public double getFdM(){return this.FdM;}
final public double getepsM(){return this.epsM;}final public double getpiM(){return this.piM;}
final public double getvFM(){return this.vFM;}final public double getvBM(){return this.vBM;}
final public double getFilamentLength(){return this.filamentLength;}
final public boolean getChangedMotorNumber(){return changedMotorNumber;}

final public void setCargoParas(int Np,int Nm,
	double FsP,double FdP,double epsP,double piP,double vFP, double vBP,
	double FsM,double FdM,double epsM,double piM,double vFM, double vBM){
	setCargoParas(Np,Nm,FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM,xStart,tStart,dtStart);
}
final public void setCargoParas(int Np,int Nm,
	double FsP,double FdP,double epsP,double piP,double vFP, double vBP,
	double FsM,double FdM,double epsM,double piM,double vFM, double vBM,
	double x,double t,double delT){
	// set motor parameters
		this.Np=Np;this.Nm=Nm;
		this.FsP=FsP;this.FdP=FdP;this.epsP=epsP;this.piP=piP;this.vFP=vFP;this.vBP=vBP;
		this.FsM=FsM;this.FdM=FdM;this.epsM=epsM;this.piM=piM;this.vFM=vFM;this.vBM=vBM;
		this.x=x;this.t=t;this.dt=delT;this.xAbs=0.0;
		this.np=Np;this.nm=Nm;
		changedMotorNumber=false;

	// Assert that the binding probabilities are smaller than 1
	// Don't do this for the off probabilities, because they can be very large, leading to a very small dt.
	//		Instead perform immediate drop off for large off probabilities
		while((Np*piP+Nm*piM)*dt>=1){dt/=2.;}
	
// set transition rates	
	offProbaPlus=null;offProbaMinus=null;onProbaPlus=null;onProbaMinus=null;//hopProba=null;
	offProbaPlus = new double[Np+1][Nm+1];offProbaMinus = new double[Np+1][Nm+1];
	onProbaPlus = new double[Np+1][Nm+1];onProbaMinus = new double[Np+1][Nm+1];
	//hopProba = new double[Np+1][Nm+1];
	for(int kp=0;kp<=Np;kp++){for(int km=0;km<=Nm;km++){
		offProbaPlus[kp][km]=dt*epsP*kp*Math.exp(plusMotorForce(kp,km)/FdP);
		offProbaMinus[kp][km]=dt*epsM*km*Math.exp(minusMotorForce(kp,km)/FdM);
		onProbaPlus[kp][km]=dt*(Np-kp)*piP;
		onProbaMinus[kp][km]=dt*(Nm-km)*piM;
	}}
// 	System.out.println("\n");
// 	System.out.println("Cagro rates:");
// 	for(int kp=0;kp<=Np;kp++){for(int km=0;km<=Nm;km++){
// 		System.out.println("("+kp+","+km+"): "
// 			+     doubleFormat2.format(offProbaPlus[kp][km]/dt)
// 			+", "+doubleFormat2.format(offProbaMinus[kp][km]/dt)
// 			+", "+doubleFormat2.format(onProbaPlus[kp][km]/dt)
// 			+", "+doubleFormat2.format(onProbaMinus[kp][km]/dt));
// 	}}
// 	System.out.println("Cagro probabilities:");
// 	for(int kp=0;kp<=Np;kp++){for(int km=0;km<=Nm;km++){
// 		System.out.println("("+kp+","+km+"): "
// 			+     doubleFormat2.format(offProbaPlus[kp][km])
// 			+", "+doubleFormat2.format(offProbaMinus[kp][km])
// 			+", "+doubleFormat2.format(onProbaPlus[kp][km])
// 			+", "+doubleFormat2.format(onProbaMinus[kp][km]));
// 	}}
}// end setCargoParas

private final double cargoForce(int kp,int km){
	if((kp==0)||(km==0)){return 0.;}
	else{ 
		double lambda; double F;
		if(kp*FsP>=km*FsM)	{lambda=1./(1.+(kp*FsP/vFP)/(km*FsM/vBM));}
		else 			{lambda=1./(1.+(kp*FsP/vBP)/(km*FsM/vFM));}
		F=lambda*kp*FsP+(1.-lambda)*km*FsM; if(F<0){System.out.println("F<0");}
		return (lambda*kp*FsP+(1.-lambda)*km*FsM);
	}
}
private final double plusMotorForce(int kp,int km){if(kp>0){return cargoForce(kp,km)/kp;}else{return 0;}}
private final double minusMotorForce(int kp,int km){if(km>0){return cargoForce(kp,km)/km;}else{return 0;}}
private final double velocity(int kp,int km){
	if((kp==0)&&(km==0)){return 0.;}
//	else if(kp*FsP>=km*FsM){return vFP*(1-cargoForce(kp,km)/kp/FsP);}
//	else  {return -vFM*(1-cargoForce(kp,km)/km/FsM);}
	else if(kp*FsP>=km*FsM){return (kp*FsP-km*FsM)/(kp*FsP/vFP + km*FsM/vBM);}
	else  {return (kp*FsP-km*FsM)/(kp*FsP/vBP + km*FsM/vFM);}
}

public void makeStep(){

	changedMotorNumber=false;
	t+=dt;
	x+=dt*velocity(np,nm);
	xAbs+=Math.abs(dt*velocity(np,nm));
	rand=Math.random();

// This is for cases where the offProba's are very large.
//	They can't be ensured to be small enough because then the time step would be too slow for the animation
//	sum=offProbaPlus[np][nm]+offProbaMinus[np][nm]+onProbaPlus[np][nm]+onProbaMinus[np][nm];
//	if(sum>1){
//		if((offProbaPlus[np][nm]>=1)&&(offProbaMinus[np][nm]>=1)){randPM=Math.random();}
//		else if(offProbaPlus[np][nm]>offProbaMinus[np][nm]){randPM=0.6;}	// test plus motor unbinding first
//		else if(offProbaPlus[np][nm]<offProbaMinus[np][nm]){randPM=0.4;} // test minus motor unbinding first
//		else{randPM=Math.random();} 
//	}
//	else{randPM=Math.random();}

	randPM=Math.random(); // this is necessary since off probabilities can become very large (>1)
		//They can't be ensured to be small enough because then the time step would be too slow for the animation

	if(randPM>0.5){// test plus motor dissociation first
		prob=offProbaPlus[np][nm];
		if(rand<prob)							{np-=1;changedMotorNumber=true;}
		else if(rand<(prob+=offProbaMinus[np][nm]))	{nm-=1;changedMotorNumber=true;}
		else if(rand<(prob+=onProbaPlus[np][nm]))	{np+=1;changedMotorNumber=true;}
		else if(rand<(prob+=onProbaMinus[np][nm]))	{nm+=1;changedMotorNumber=true;}
	}
	else{// test minus motor dissociation first
		prob=offProbaMinus[np][nm];
		if(rand<prob)							{nm-=1;changedMotorNumber=true;}
		else if(rand<(prob+=offProbaPlus[np][nm]))	{np-=1;changedMotorNumber=true;}
		else if(rand<(prob+=onProbaPlus[np][nm]))	{np+=1;changedMotorNumber=true;}
		else if(rand<(prob+=onProbaMinus[np][nm]))	{nm+=1;changedMotorNumber=true;}
	}


}// end makeStep

public void displayRates(){
	for(int kp=0;kp<=Np;kp++){for(int km=0;km<=Nm;km++){
		System.out.println("(kp,km)=("+kp+","+km+")"+": F="+cargoForce(kp,km)+", v = "+velocity(kp,km)
			+", epsP="+epsP*kp*Math.exp(plusMotorForce(kp,km)/FdP)+", epsM="+epsM*km*Math.exp(minusMotorForce(kp,km)/FdM)
			+", piP="+onProbaPlus[kp][km]/dt+", piM="+onProbaMinus[kp][km]/dt);
	}}
}

}// end class Cargo