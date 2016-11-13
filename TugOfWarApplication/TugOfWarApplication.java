/** Java application for animation of tug-of-war of several plus and minus motors on a common cargo.
 *
 *  Written by Melanie J.I. Mueller, Max Planck Institute of Colloids and Interfaces, September 2008
 *
 *  Needs Cargo.java (tug-of-war calculation), GraphicsUtil.java 
 */

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.text.*;
import javax.swing.*;
import javax.swing.border.LineBorder;


public class TugOfWarApplication extends Panel implements Runnable,ActionListener{

final static int LWindow=Toolkit.getDefaultToolkit().getScreenSize().width; // window width = screen size
final static int HWindow=620; // window height

final static int L=LWindow; // canvas width = window width
final static int H=390; 	// canvas height

//=============================================================
//=============================================================
// MAIN 
//=============================================================
//=============================================================
public static void main(String[] args) {

  Frame f = new Frame("Tug-of-war animation");
  f.addWindowListener(new java.awt.event.WindowAdapter() {
       public void windowClosing(java.awt.event.WindowEvent e) {
       System.exit(0);
       };
     });
  TugOfWarApplication tugPanel = new TugOfWarApplication();
  tugPanel.setSize(LWindow,HWindow); // same size as defined in the HTML APPLET
  f.add(tugPanel);
  f.pack();
  tugPanel.init();
  tugPanel.start();
  f.setSize(LWindow,HWindow + 20); // add 20, seems enough for the Frame title,
  f.setVisible(true);
}
//=============================================================


// animation parameters
int sleepTime=100;
int timeFactor=1;//real time is slowed down by that factor

// parameters for moving cargo
final int filamentY=320; 			// where the y position of the filament is in the canvas [pixel]
final int cargoRadius=15; 			// cargo radius [pixel]
final int filamentHalfThickness=3;		// half thickness of filament [pixel]
final int NMax=10;					// maximum number of motors of one type
final double vCut=0.00;				// cutoff velocity [um/sec]

// parameters for magnified cargo
final int magFilamentHeight=180, magFilamentHalfThickness=10; // y-position  and thickness of filament
int magCargoCenterX;//=Math.round(L/2); //cargo center position
int magCargoHalfWidth,magCargoHalfHeight;// cargo sizes, depend on motor numbers
final int motorTopOffset=30,motorBottomOffset=35,headOffset=5; // how motor is offset with respect to cargo,filament
final int headRadius=9,linkerLength=7;	// head radius, linker length <-> head distance
final int stalkThickness=8,linkerThickness=4;		// line thicknesses of stalk, linker
final int halfStalkThickness=Math.round(stalkThickness/2);
final int motorLift=45;	// how much an unbound motor is lifted above the filament
final int motorDistance=55; // distance of motors on the cargo
Color stalkColor=Color.gray,plusMotorColor=Color.red,minusMotorColor=Color.green;
int motorRightOffset;	// how much head is offset to tail (cargo binding region)
int pos;			// motor position
boolean bound;		// whether a motor is bound or not
final int arrowMaxLength=200,arrowOffsetX=25,arrowOffsetY=15,arrowHeight=magFilamentHeight+40,arrowThickness=8;
	// arrow maximal length, x and y offset of arrow head, arrow y position, arrow thickness
int arrowLength;	// actual arrow length (depends on cargo velocity)


// cargo and motor parameters
int Np,Nm; double FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM;
Cargo cargo;						// cargo of Cargo class, does calculation
int xPix;				// cargo x, start position [pixel]

// graphics variables
Thread tugAnimation;
Canvas canvas;
Image dbi;
Graphics g,dbg;

// formats
Font font=new Font("Helvetica",Font.BOLD,15);
Font fontSmall=new Font("Helvetica",Font.BOLD,12);
Font fontLarge=new Font("Helvetica",Font.BOLD,24);
DecimalFormat doubleFormat1 = new DecimalFormat("##0.0"); // formatting for output of doubles
DecimalFormat doubleFormat2 = new DecimalFormat("##0.00"); // formatting for output of doubles
DecimalFormat doubleFormat3 = new DecimalFormat("##0.000"); // formatting for output of doubles

// Control buttons and text fields
JCheckBox cb_startHold;
JButton but_startHold,but_restart,but_quit;
Button but_paraInfo,but_help,but_info;;
Choice choice_timeFactor;
JLabel timeLab,txt_returnMessage,txt_copyright; 
JLabel lab_Np,lab_Nm,lab_FsP,lab_FdP,lab_epsP,lab_piP,lab_vFP,lab_vBP,lab_FsM,lab_FdM,lab_epsM,lab_piM,lab_vFM,lab_vBM;
JTextField txt_Np,txt_Nm,txt_FsP,txt_FdP,txt_epsP,txt_piP,txt_vFP,txt_vBP,txt_FsM,txt_FdM,txt_epsM,txt_piM,txt_vFM,txt_vBM;
JTextArea txt_message;

boolean run;
//=======================================================================
// initialize
public void init(){
	// set up canvas, buffer image, graphics
		canvas=new Canvas();canvas.setSize(L,H);add(canvas);
		g=canvas.getGraphics();
		setBackground(Color.decode("#FFF8DC"));		// light rose-yellow
		cargo=new Cargo();	// create cargo with default parameters
		magCargoCenterX=Math.round(L/2); //cargo center position
		dbi=createImage(L,H);
		dbg=dbi.getGraphics();
		dbg.setFont(font);
	// set up animation elements 
		getCargoParas();	// set animation parameters to cargo parameters
		drawStartState();	// draw all basic grahic elements
	// set up control buttons and text fields
		Color panelColor=getBackground();
		JPanel organizePanel=new JPanel();
			organizePanel.setLayout(new BoxLayout(organizePanel,BoxLayout.PAGE_AXIS));	// panel that organizes all the other panels
			organizePanel.setBackground(panelColor);add(organizePanel);
		JPanel messagePanel=new JPanel();			// Panel for messages
			messagePanel.setBackground(panelColor);organizePanel.add(messagePanel);//,BorderLayout.PAGE_START);
		JPanel buttonPanel=new JPanel();					// panel for the control buttons
			buttonPanel.setBackground(panelColor);organizePanel.add(buttonPanel);//,BorderLayout.CENTER);
		JPanel motorParaPanel=new JPanel(new GridLayout(3,6,-5,0));			// panel for motor parameter text fields
			motorParaPanel.setBackground(panelColor);organizePanel.add(motorParaPanel);//,BorderLayout.PAGE_END);
		txt_message=new JTextArea(" Press the Run/Pause button to start the simulation.",2,50);txt_message.setFont(font);
			txt_message.setBackground(Color.white);
			messagePanel.add(txt_message);
		but_startHold=new JButton("Run/Pause");but_startHold.setFont(font);
			buttonPanel.add(but_startHold);but_startHold.addActionListener(this);run=false;
		but_restart=new JButton("Reset");but_restart.setFont(font);
			buttonPanel.add(but_restart);but_restart.addActionListener(this);	
		timeLab=new JLabel("     Time slowed down by ");timeLab.setFont(font);buttonPanel.add(timeLab);
		choice_timeFactor= new Choice();choice_timeFactor.setFont(font);
			choice_timeFactor.add("1");choice_timeFactor.add("10");choice_timeFactor.add("100");
			if(choice_timeFactor.getItem(0).equals(String.valueOf(timeFactor))){choice_timeFactor.select(0);}
			else if(choice_timeFactor.getItem(1).equals(String.valueOf(timeFactor))){choice_timeFactor.select(1);}
			else if(choice_timeFactor.getItem(2).equals(String.valueOf(timeFactor))){choice_timeFactor.select(2);}
			else{timeFactor=1;}	
			choice_timeFactor.addItemListener(new TimeChoiceListener());
			buttonPanel.add(choice_timeFactor);
		but_paraInfo=new Button("Simulation info");but_paraInfo.setFont(font);
			buttonPanel.add(but_paraInfo);but_paraInfo.addActionListener(this);	
		but_info=new Button("Author info");but_info.setFont(font);
			buttonPanel.add(but_info);but_info.addActionListener(this);	
		JPanel emptyPanel1=new JPanel();emptyPanel1.setBackground(panelColor);motorParaPanel.add(emptyPanel1);
		JPanel emptyPanel2=new JPanel();emptyPanel2.setBackground(panelColor);motorParaPanel.add(emptyPanel2);
		JPanel NpPanel=new JPanel();NpPanel.setBackground(panelColor);motorParaPanel.add(NpPanel);
			lab_Np=new JLabel("<html> &nbsp;&nbsp; N<sub>+</sub> = </html>");NpPanel.add(lab_Np);
 			txt_Np=new JTextField(String.valueOf(Np),2);NpPanel.add(txt_Np);txt_Np.addActionListener(this);
		JPanel NmPanel=new JPanel();NmPanel.setBackground(panelColor);motorParaPanel.add(NmPanel);
			lab_Nm=new JLabel("<html> &nbsp;&nbsp; N<sub>-</sub> = </html>");NmPanel.add(lab_Nm);
	 		txt_Nm=new JTextField(String.valueOf(Nm),2);NmPanel.add(txt_Nm);txt_Nm.addActionListener(this);
		JPanel emptyPanel3=new JPanel();emptyPanel3.setBackground(panelColor);motorParaPanel.add(emptyPanel3);
		JPanel emptyPanel4=new JPanel();emptyPanel4.setBackground(panelColor);motorParaPanel.add(emptyPanel4);
		JPanel FsPPanel=new JPanel();FsPPanel.setBackground(panelColor);motorParaPanel.add(FsPPanel);
			lab_FsP=new JLabel("<html> &nbsp;&nbsp; F<sub>s+</sub> [pN] = </html>");FsPPanel.add(lab_FsP);
			txt_FsP=new JTextField(doubleFormat1.format(FsP),3);FsPPanel.add(txt_FsP);txt_FsP.addActionListener(this);
		JPanel FdPPanel=new JPanel();FdPPanel.setBackground(panelColor);motorParaPanel.add(FdPPanel);
			lab_FdP=new JLabel("<html> &nbsp;&nbsp; F<sub>d+</sub> [pN] = </html>");FdPPanel.add(lab_FdP);
			txt_FdP=new JTextField(doubleFormat1.format(FdP),3);FdPPanel.add(txt_FdP);txt_FdP.addActionListener(this);
		JPanel epsPPanel=new JPanel();epsPPanel.setBackground(panelColor);motorParaPanel.add(epsPPanel);
			lab_epsP=new JLabel("<html> &nbsp;&nbsp; \u03B5<sub>0+</sub> [1/s] = </html>");epsPPanel.add(lab_epsP);
			txt_epsP=new JTextField(doubleFormat1.format(epsP),3);epsPPanel.add(txt_epsP);txt_epsP.addActionListener(this);
		JPanel piPPanel=new JPanel();piPPanel.setBackground(panelColor);motorParaPanel.add(piPPanel);
			lab_piP=new JLabel("<html> &nbsp;&nbsp; \u03C0<sub>0+</sub> [1/s] = </html>");piPPanel.add(lab_piP);
			txt_piP=new JTextField(doubleFormat1.format(piP),3);piPPanel.add(txt_piP);txt_piP.addActionListener(this);
		JPanel vFPPanel=new JPanel();vFPPanel.setBackground(panelColor);motorParaPanel.add(vFPPanel);
			lab_vFP=new JLabel("<html> &nbsp;&nbsp; v<sub>F+</sub> [\u03BCm/s] = </html>");vFPPanel.add(lab_vFP);
			txt_vFP=new JTextField(doubleFormat1.format(vFP),3);vFPPanel.add(txt_vFP);txt_vFP.addActionListener(this);
		JPanel vBPPanel=new JPanel();vBPPanel.setBackground(panelColor);motorParaPanel.add(vBPPanel);
			lab_vBP=new JLabel("<html> &nbsp;&nbsp; v<sub>B+</sub> [nm/s] = </html>");vBPPanel.add(lab_vBP);
			txt_vBP=new JTextField(doubleFormat1.format(1000*vBP),3);vBPPanel.add(txt_vBP);txt_vBP.addActionListener(this);
		JPanel FsMPanel=new JPanel();FsMPanel.setBackground(panelColor);motorParaPanel.add(FsMPanel);
			lab_FsM=new JLabel("<html> &nbsp;&nbsp; F<sub>s-</sub> [pN] = </html>");FsMPanel.add(lab_FsM);
			txt_FsM=new JTextField(doubleFormat1.format(FsM),3);FsMPanel.add(txt_FsM);txt_FsM.addActionListener(this);
		JPanel FdMPanel=new JPanel();FdMPanel.setBackground(panelColor);motorParaPanel.add(FdMPanel);
			lab_FdM=new JLabel("<html> &nbsp;&nbsp; F<sub>d-</sub> [pN] = </html>");FdMPanel.add(lab_FdM);
			txt_FdM=new JTextField(doubleFormat1.format(FdM),3);FdMPanel.add(txt_FdM);txt_FdM.addActionListener(this);
		JPanel epsMPanel=new JPanel();epsMPanel.setBackground(panelColor);motorParaPanel.add(epsMPanel);
			lab_epsM=new JLabel("<html> &nbsp;&nbsp; \u03B5<sub>0-</sub> [1/s] = </html>");epsMPanel.add(lab_epsM);
			txt_epsM=new JTextField(doubleFormat1.format(epsM),3);epsMPanel.add(txt_epsM);txt_epsM.addActionListener(this);
		JPanel piMPanel=new JPanel();piMPanel.setBackground(panelColor);motorParaPanel.add(piMPanel);
			lab_piM=new JLabel("<html> &nbsp;&nbsp; \u03C0<sub>0-</sub> [1/s] = </html>");piMPanel.add(lab_piM);
			txt_piM=new JTextField(doubleFormat1.format(piM),3);piMPanel.add(txt_piM);txt_piM.addActionListener(this);
		JPanel vFMPanel=new JPanel();vFMPanel.setBackground(panelColor);motorParaPanel.add(vFMPanel);
			lab_vFM=new JLabel("<html> &nbsp;&nbsp; v<sub>F-</sub> [\u03BCm/s] = </html>");vFMPanel.add(lab_vFM);
			txt_vFM=new JTextField(doubleFormat1.format(vFM),3);vFMPanel.add(txt_vFM);txt_vFM.addActionListener(this);
		JPanel vBMPanel=new JPanel();vBMPanel.setBackground(panelColor);motorParaPanel.add(vBMPanel);
			lab_vBM=new JLabel("<html> &nbsp;&nbsp; v<sub>B-</sub> [nm/s] = </html>");vBMPanel.add(lab_vBM);
			txt_vBM=new JTextField(doubleFormat1.format(1000*vBM),3);vBMPanel.add(txt_vBM);txt_vBM.addActionListener(this);
		JPanel copyrightPanel=new JPanel();copyrightPanel.setBackground(panelColor);organizePanel.add(copyrightPanel);
			txt_copyright=new JLabel("<html>Press Enter after changing numbers in the table. &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &copy; Melanie J.I. M&uuml;ller 2008, MPI-KG Potsdam</html>",JLabel.RIGHT);
			txt_copyright.setFont(fontSmall);copyrightPanel.add(txt_copyright);
			UIManager.put("JOptionPane.font",font); 
		// Tool Tips
			//ToolTipManager.sharedInstance().setEnabled(false);
			UIManager.put("ToolTip.background", Color.yellow); 
			UIManager.put("ToolTip.foreground", Color.blue); 
      		UIManager.put("ToolTip.font",font);
			ToolTipManager.sharedInstance().setDismissDelay(15000);// 15 seconds    
			but_startHold.setToolTipText("Run or pause simulation");
			but_restart.setToolTipText("Set parameters back to standard values (kinesin-1)");
			timeLab.setToolTipText("Slow down simulation by factor of 10 or 100\n ");
			NpPanel.setToolTipText("number of (red) plus motors");
			NmPanel.setToolTipText("number of (green) minus motors");
			FsPPanel.setToolTipText("plus motor stall force");
			FdPPanel.setToolTipText("plus motor detachment force");
			epsPPanel.setToolTipText("plus motor unbinding rate");
			piPPanel.setToolTipText("plus motor binding rate");
			vFPPanel.setToolTipText("plus motor forward velocity");
			vBPPanel.setToolTipText("plus motor backward velocity parameter");
			FsMPanel.setToolTipText("minus motor stall force");
			FdMPanel.setToolTipText("minus motor detachment force");
			epsMPanel.setToolTipText("minus motor unbinding rate");
			piMPanel.setToolTipText("minus motor binding rate");
			vFMPanel.setToolTipText("minus motor forward velocity");
			vBMPanel.setToolTipText("minus motor backward velocity parameter");
}//end init()
//=======================================================================

//=======================================================================
//=======================================================================
// Graphics
//=======================================================================
//=======================================================================
// draw filament of length length into graphics gr, starting at xStart, at height filamentY, of thickness 2*filamentHalfThickness
// 		also redraws the background around the filament in distance 2*cargoRadius
private final void drawFilament(Graphics gr,int xStart,int length){
	//gr.setColor(getBackground());gr.fillRect(xStart,filamentY-cargoRadius,length,2*cargoRadius);
	gr.setColor(getBackground());gr.fillRect(xStart-2,filamentY-(cargoRadius+2),length+4,2*(cargoRadius+4));
		// fill rectangle of cargo size with background colour
	//gr.setColor(Color.gray);	gr.fillRect(xStart,filamentY-filamentHalfThickness,length,2*filamentHalfThickness);
	gr.setColor(Color.gray);	gr.fillRect(xStart-2,filamentY-filamentHalfThickness,length+4,2*filamentHalfThickness);
		// draw filament in this rectangle

}

// draw the moving cargo at position xPos into graphics gr, 
//		on filament at height filamentY. Cargo is sphere with radius cargoRadius
private final void drawCargo(Graphics gr,int xPos){
	gr.setColor(Color.blue);
	gr.fillOval(xPos-cargoRadius,filamentY-cargoRadius,2*cargoRadius,2*cargoRadius);
}

// display status of the moving cargo
private final void displayCargoInfo(Graphics gr){
	int textheight=filamentY+40;
	gr.setFont(font);	gr.setColor(getBackground());
	gr.fillRect(0,textheight-15,L,22);
	gr.setColor(Color.black);
	gr.drawString(
		"Time t = "		+doubleFormat2.format(cargo.getTime()/60)+" min."
		+" Position x = "+doubleFormat2.format(cargo.getPosition())+" \u03BCm."
		+" Absolute travelled distance = "+doubleFormat2.format(cargo.getDistanceTravelled())+" \u03BCm."
		,50,textheight);
}

// draw the  magnifieed cargo with given motor numbers
private final void drawMagnifiedCargo(Graphics gr,int Np,int Nm,int np,int nm){
		int kp,km;
	// delete background
		gr.setColor(getBackground());	gr.fillRect(0,0,L,arrowHeight+arrowOffsetY+25);
	// cargo
		gr.setColor(Color.blue);
		magCargoHalfWidth=Math.round(Math.max(Np,Nm)*motorDistance+20);magCargoHalfHeight=50;
		gr.fillArc(magCargoCenterX-magCargoHalfWidth,0-magCargoHalfHeight,2*magCargoHalfWidth,2*magCargoHalfHeight,180,180);
	// filament
		gr.setColor(Color.gray);gr.fillRect(0,magFilamentHeight-magFilamentHalfThickness,L,2*magFilamentHalfThickness);
	// plus motors
		pos=magCargoCenterX+20;bound=true;
		for(kp=0;kp<np;kp++){drawPlusMotor(gr,pos+kp*motorDistance,bound);}
		bound=false;
		for(kp=np;kp<Np;kp++){drawPlusMotor(gr,pos+kp*motorDistance,bound);}
	// minus motors
		pos=magCargoCenterX-20;bound=true;
		for(km=0;km<nm;km++){drawMinusMotor(gr,pos-km*motorDistance,bound);}
		bound=false;
		for(km=nm;km<Nm;km++){drawMinusMotor(gr,pos-km*motorDistance,bound);}
	// arrow for direction of motion
		double v=cargo.getVelocity();
		if(v>vCut){
			arrowLength=round((v/vFP)*arrowMaxLength);
			pos=magCargoCenterX+90;	
			drawPositiveArrow(gr,pos,arrowHeight,arrowLength);
		}
		else if(v<-vCut){
			arrowLength=round((-v/vFM)*arrowMaxLength);
			pos=magCargoCenterX-90;	
			drawNegativeArrow(gr,pos,arrowHeight,arrowLength);
		}
		else{}// else draw no arrow
	// display cargo velocity
		gr.setColor(Color.black);
		gr.drawString("velocity ",magCargoCenterX-35,arrowHeight-10);
		gr.drawString("v = "+doubleFormat2.format(cargo.getVelocity())+" \u03BCm/s",magCargoCenterX-55,arrowHeight+10);
	// display forces on motors
		gr.setColor(plusMotorColor);
		pos=magCargoCenterX+50;	
		gr.drawString("Force on each plus motor = "+doubleFormat1.format(cargo.getFp())+" pN",pos,arrowHeight+arrowOffsetY+20);
		gr.setColor(minusMotorColor);
		pos=magCargoCenterX-140-arrowMaxLength;	
		gr.drawString("Force on each minus motor = "+doubleFormat1.format(cargo.getFm())+" pN",pos,arrowHeight+arrowOffsetY+20);
}// end drawMagnifiedCargo
private final void drawPositiveArrow(Graphics gr,int pos,int arrowHeight,int arrowLength){
	gr.setColor(plusMotorColor);
	GraphicsUtil.drawLine(gr,pos,arrowHeight,pos+arrowLength,arrowHeight,arrowThickness);
	int[] xPoints = {pos+arrowLength+2*arrowThickness,pos+arrowLength-arrowOffsetX,pos+arrowLength-arrowOffsetX},
			yPoints={arrowHeight,arrowHeight-arrowOffsetY,arrowHeight+arrowOffsetY};
	gr.fillPolygon(xPoints,yPoints,3);
}

private final void drawNegativeArrow(Graphics gr,int pos,int arrowHeight,int arrowLength){
	gr.setColor(minusMotorColor);
	GraphicsUtil.drawLine(gr,pos,arrowHeight,pos-arrowLength,arrowHeight,arrowThickness);
	int[] xPoints = {pos-arrowLength-2*arrowThickness,pos-arrowLength+arrowOffsetX,pos-arrowLength+arrowOffsetX},
			yPoints={arrowHeight,arrowHeight-arrowOffsetY,arrowHeight+arrowOffsetY};
	gr.fillPolygon(xPoints,yPoints,3);
}

private final void drawPlusMotor(Graphics gr,int pos,boolean bound){
	if(bound){	// draw bound motor
		motorRightOffset=70;
		GraphicsUtil.drawLine(gr,pos,magCargoHalfHeight-motorTopOffset,
				pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset,
				stalkThickness,stalkColor);	// motor stalk
		GraphicsUtil.drawLine(gr,pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset,
				pos+motorRightOffset-linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				linkerThickness,stalkColor);	// linker to left motor head
		GraphicsUtil.drawLine(gr,pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset,
				pos+motorRightOffset+2*linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				linkerThickness,stalkColor);	// linker to right motor head
		GraphicsUtil.fillCircle(gr,pos+motorRightOffset-linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				headRadius,plusMotorColor);	// left motor head
		GraphicsUtil.fillCircle(gr,pos+motorRightOffset+2*linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				headRadius,plusMotorColor);	// right motor head
	}
	else{		// draw unbound motor
		motorRightOffset=111;
		GraphicsUtil.drawLine(gr,pos,magCargoHalfHeight-motorTopOffset,
				pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				stalkThickness,stalkColor);	// motor stalk
		GraphicsUtil.drawLine(gr,pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				pos+motorRightOffset+linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-headOffset,
				linkerThickness,stalkColor);	// linker to left motor head
		GraphicsUtil.drawLine(gr,pos+motorRightOffset+halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				pos+motorRightOffset+3*linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-3*linkerLength,
				linkerThickness,stalkColor);	// linker to right motor head
		GraphicsUtil.fillCircle(gr,
				pos+motorRightOffset+linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-headOffset,
				headRadius,plusMotorColor);	// left motor head
		GraphicsUtil.fillCircle(gr,
				pos+motorRightOffset+3*linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-3*linkerLength,
				headRadius,plusMotorColor);	// right motor head
	}
}// end drawPlusMotor

private final void drawMinusMotor(Graphics gr,int pos,boolean bound){
	if(bound){	// draw bound motor
		motorRightOffset=70;
		GraphicsUtil.drawLine(gr,pos,magCargoHalfHeight-motorTopOffset,
				pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset,
				stalkThickness,stalkColor);	// motor stalk
		GraphicsUtil.drawLine(gr,pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset,
				pos-motorRightOffset+linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				linkerThickness,stalkColor);	// linker to left motor head
		GraphicsUtil.drawLine(gr,pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset,
				pos-motorRightOffset-2*linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				linkerThickness,stalkColor);	// linker to right motor head
		GraphicsUtil.fillCircle(gr,pos-motorRightOffset+linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				headRadius,minusMotorColor);	// left motor head
		GraphicsUtil.fillCircle(gr,pos-motorRightOffset-2*linkerLength,magFilamentHeight-magFilamentHalfThickness-headOffset,
				headRadius,minusMotorColor);	// right motor head
	}
	else{		// draw unbound motor
		motorRightOffset=111;
		GraphicsUtil.drawLine(gr,pos,magCargoHalfHeight-motorTopOffset,
				pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				stalkThickness,stalkColor);	// motor stalk
		GraphicsUtil.drawLine(gr,pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				pos-motorRightOffset-linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-headOffset,
				linkerThickness,stalkColor);	// linker to left motor head
		GraphicsUtil.drawLine(gr,pos-motorRightOffset-halfStalkThickness,magFilamentHeight-motorBottomOffset-motorLift,
				pos-motorRightOffset-3*linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-3*linkerLength,
				linkerThickness,stalkColor);	// linker to right motor head
		GraphicsUtil.fillCircle(gr,
				pos-motorRightOffset-linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-headOffset,
				headRadius,minusMotorColor);	// left motor head
		GraphicsUtil.fillCircle(gr,
				pos-motorRightOffset-3*linkerLength,magFilamentHeight-magFilamentHalfThickness-motorLift-3*linkerLength,
				headRadius,minusMotorColor);	// right motor head
	}
}// end drawPlusMotor

// display status of the magnified cargo
private final void displayMagnifiedCargoInfo(Graphics gr){
	int textheight=filamentY-100;
	gr.setFont(font);	gr.setColor(getBackground());
	gr.fillRect(0,textheight-20,L,40);
	gr.setColor(Color.black);
	gr.drawString(
		"number of bound (plus, minus) motors = ("+cargo.getnp()+", "+cargo.getnm()+")"
		+",  force on single (plus, minus) motor = ("
				+doubleFormat1.format(cargo.getFp())+" pN, "+doubleFormat1.format(cargo.getFm())+" pN)"
		+", v = "+convertToPixelPerSec(cargo.getVelocity())+" pix/sec"
		,10,textheight);
}

private final void displaySimulationInfo(Graphics gr){
	GraphicsUtil.drawLine(gr,0,magFilamentHeight+90,L,magFilamentHeight+90,2);
	gr.drawString("Above: magnifed view of the (blue) cargo transported by (green) minus and (red) plus motors, "
				+"which can bind to and unbind from the (gray) filament.",50,magFilamentHeight+120);
	gr.drawString("Below: microscope-like view of the (blue) cargo, "
				+"which moves along the (gray) filament of length "
				+ doubleFormat1.format(cargo.getFilamentLength())+" \u03BCm.",50,magFilamentHeight+140);
	GraphicsUtil.drawLine(gr,0,magFilamentHeight+160,L,magFilamentHeight+160,2);
	GraphicsUtil.drawLine(gr,0,filamentY+60,L,filamentY+60,2);
}

private final void drawStartState(){
	// parameters	
		xPix=convertToPixel(cargo.getPosition());
	// drawing
		dbg.setColor(getBackground());dbg.fillRect(0,0,L,H);	//background
		drawFilament(dbg,0,L);				// filament
		drawMagnifiedCargo(dbg,Np,Nm,Np,Nm);	// magnified cargo with motors
		dbg.setColor(Color.black);
		GraphicsUtil.drawLine(dbg,0,filamentY-50,L,filamentY-50,2);
		GraphicsUtil.drawLine(dbg,0,filamentY+50,L,filamentY+50,2);
		drawCargo(dbg,xPix);				// moving cargo
		displayCargoInfo(dbg);				// info about moving cargo
		g.drawImage(dbi,0,0,this); 			// draw buffer image to screen
}

private final void getCargoParas(){
	Np=cargo.getNp();Nm=cargo.getNm();
	FsP=cargo.getFsP();FdP=cargo.getFdP();epsP=cargo.getepsP();piP=cargo.getpiP();vFP=cargo.getvFP();vBP=cargo.getvBP();
	FsM=cargo.getFsM();FdM=cargo.getFdM();epsM=cargo.getepsM();piM=cargo.getpiM();vFM=cargo.getvFM();vBM=cargo.getvBM();
	sleepTime=round(cargo.getdt()*1000*timeFactor); // this ensures that the simulation runs in real time:
		// The sleep time sleepTime for each step [ms] is equal to the time step dt [s].
		// This assumes that the calculation itself takes only negligible time.
	if(sleepTime<1){sleepTime=1;}
}
private final void setJTextFieldCargoParas(){
	txt_Np.setText(String.valueOf(cargo.getNp()));txt_Nm.setText(String.valueOf(cargo.getNm()));
	txt_FsP.setText(String.valueOf(cargo.getFsP()));txt_FdP.setText(String.valueOf(cargo.getFdP()));
	txt_epsP.setText(String.valueOf(cargo.getepsP()));txt_piP.setText(String.valueOf(cargo.getpiP()));
	txt_vFP.setText(String.valueOf(cargo.getvFP()));txt_vBP.setText(String.valueOf(cargo.getvBP()*1000));
	txt_FsM.setText(String.valueOf(cargo.getFsM()));txt_FdM.setText(String.valueOf(cargo.getFdM()));
	txt_epsM.setText(String.valueOf(cargo.getepsM()));txt_piM.setText(String.valueOf(cargo.getpiM()));
	txt_vFM.setText(String.valueOf(cargo.getvFM()));txt_vBM.setText(String.valueOf(cargo.getvBM()*1000));
}

private final void displayCargoParas(Graphics gr){
	gr.setColor(getBackground());gr.fillRect(0,360-20,L,40);
	gr.setColor(Color.black);
	gr.drawString(
		"Np="+Np+", Nm="+Nm
		+", FsP="+FsP+", FdP="+FdP+", epsP="+epsP+", piP="+piP+", vFP="+vFP+", vBP="+vBP
		+", FsM="+FsM+", FdM="+FdM+", epsM="+epsM+", piM="+piM+", vFM="+vFM+", vBM="+vBM,10,360);
	gr.drawString("dt="+cargo.getdt()+", sleepTime="+sleepTime+", real time slowed down by "+timeFactor,10,380);
}

// conversion of micrometers -> pixel 
//	attention: filament has x=0 in the middle. xPix=0 of course at left boundary.
private final int convertToPixel(double x){
	int xPix=round(((x*L/cargo.getFilamentLength())+L/2)%L);
	if(xPix<0){xPix=L+xPix;}
	return (xPix);
}
private final double convertToPixelPerSec(double v){return v*L/cargo.getFilamentLength();}
private final int round(double a){return (int)(Math.round(a));}
private final int drawArrow(double v){
	double vPix=v*L/cargo.getFilamentLength();
	if(vPix>vCut){return 1;}
	else if(vPix<-vCut){return -1;}
	else{return 0;}
}

//=======================================================================
//=======================================================================
// Thread tugAnimation
//=======================================================================
//=======================================================================
public void run(){

	while(tugAnimation!=null)
	{
		// start/hold 
			while(run==false)try{Thread.sleep(100);} catch(InterruptedException e){}

		// calculation, draw moving cargo and info
			cargo.makeStep();		// calculation
			drawFilament(dbg,xPix-cargoRadius,2*cargoRadius);	// overdraw old cargo in buffer
			xPix=convertToPixel(cargo.getPosition());
			drawCargo(dbg,xPix);	// draw new cargo into buffer
			displayCargoInfo(dbg);
			//displayCargoParas(dbg);
		g.drawImage(dbi,0,0,this); // draw buffer image to screen

		// draw magnified cargo and info, this should be done only when (np,nm) change
		if(cargo.getChangedMotorNumber()){
			drawMagnifiedCargo(dbg,Np,Nm,cargo.getnp(),cargo.getnm());
			//displayMagnifiedCargoInfo(dbg);
		}

		// wait for action
			try{Thread.sleep(sleepTime);} catch(InterruptedException e){}

	}

}// end run
public void start()
{
	tugAnimation=new Thread(this);
	tugAnimation.start();
}
public void pleaseStop(){
	tugAnimation=null;
}

//=======================================================================
//=======================================================================
// Actions
//=======================================================================
//=======================================================================
public void actionPerformed(ActionEvent event){

	String str;

	if(event.getActionCommand()==but_startHold.getActionCommand()){ 			// run/pause button
		if(run==false){
			run=true;
			txt_message.setForeground(Color.black);
			txt_message.setText(" Running simulation. Click the Run/Pause button to pause. "
				+"\n You can change the motor parameters listed below (number + Enter).");
		}
		else{
			run=false;
			txt_message.setForeground(Color.black);
			txt_message.setText(" Pausing simulation. Click the Run/Pause to continue. ");
		}	
	}
	else if(event.getActionCommand()==but_restart.getActionCommand()){ 			// Restart button
		run=false;
		cargo.resetCargoParas();
		setJTextFieldCargoParas();//	getCargoParas();
		drawStartState();
		txt_message.setForeground(Color.black);
		txt_message.setText(" Parameters set back to standard (kinesin) values. ");
		//displayCargoParas();
	}
	else if(event.getActionCommand()==but_paraInfo.getActionCommand()){ 			// Simulation Info button
		JOptionPane.showMessageDialog(null,
			"Java application for tug-of-war animation.\n"
			+"Filament length: 10 \u03BCm in total, periodic boundary conditions.\n"
			+"Simulation time equals real time if not slowed down by factor of 10 or 100,\n"
			+"           and if computer not busy otherwise.\n"
			+"Arrow: length and direction proportional to cargo velocity."
			, "Simulation information",JOptionPane.INFORMATION_MESSAGE);
	}
	else if(event.getActionCommand()==but_info.getActionCommand()){ 			// Author Info button
		JOptionPane.showMessageDialog(null,
			"Written by Melanie J.I. M\u00FCller, August 2008\n"
			+"Lipowsky group, Department of Theory & Bio-Systems\n"
			+"Max Planck Institute of Colloids and Interfaces, Potsdam, Germany\n"
			+"http://www.mpikg.mpg.de/th/people/mmueller/\n"
			+"Contact: mmueller@mpikg.mpg.de"
			, "Author information",JOptionPane.INFORMATION_MESSAGE);
	}
	else {												// parameter change in one of the text fields
 		str=event.getActionCommand();
		txt_message.setForeground(Color.black);
		txt_message.setText(" Changed motor parameters according to your input. ");
		try{Np=Integer.parseInt(txt_Np.getText());positiveIntegerCheck(Np);motorNumberCheck(Np);}
			catch(NumberFormatException e){Np=handleIntegerException(cargo.getNp(),txt_Np);}
			catch(NegativeIntegerException e){Np=handleIntegerException(cargo.getNp(),txt_Np);}	
			catch(LargeMotorNumberException e){Np=handleMotorNumberException(cargo.getNp(),txt_Np);}
		try{FsP=Double.valueOf(txt_FsP.getText()).doubleValue();positiveDoubleCheck(FsP);}
			catch(NumberFormatException e){FsP=handleDoubleException(cargo.getFsP(),txt_FsP);}
			catch(NegativeDoubleException e){FsP=handleDoubleException(cargo.getFsP(),txt_FsP);}
		try{FdP=Double.valueOf(txt_FdP.getText()).doubleValue();positiveDoubleCheck(FdP);}
			catch(NumberFormatException e){FdP=handleDoubleException(cargo.getFdP(),txt_FdP);}
			catch(NegativeDoubleException e){FdP=handleDoubleException(cargo.getFdP(),txt_FdP);}
		try{epsP=Double.valueOf(txt_epsP.getText()).doubleValue();positiveDoubleCheck(epsP);}
			catch(NumberFormatException e){epsP=handleDoubleException(cargo.getepsP(),txt_epsP);}
			catch(NegativeDoubleException e){epsP=handleDoubleException(cargo.getepsP(),txt_epsP);}
		try{piP=Double.valueOf(txt_piP.getText()).doubleValue();positiveDoubleCheck(piP);}
			catch(NumberFormatException e){piP=handleDoubleException(cargo.getpiP(),txt_piP);}
			catch(NegativeDoubleException e){piP=handleDoubleException(cargo.getpiP(),txt_piP);}
		try{vFP=Double.valueOf(txt_vFP.getText()).doubleValue();positiveDoubleCheck(vFP);}
			catch(NumberFormatException e){vFP=handleDoubleException(cargo.getvFP(),txt_vFP);}
			catch(NegativeDoubleException e){vFP=handleDoubleException(cargo.getvFP(),txt_vFP);}
		try{vBP=Double.valueOf(txt_vBP.getText()).doubleValue()/1000.;positiveDoubleCheck(vBP);}
			catch(NumberFormatException e){vBP=handleDoubleException(cargo.getvBP()*1000,txt_vBP);vBP/=1000.;}
			catch(NegativeDoubleException e){vBP=handleDoubleException(cargo.getvBP()*1000,txt_vBP);vBP/=1000.;}
		try{Nm=Integer.parseInt(txt_Nm.getText());positiveIntegerCheck(Nm);motorNumberCheck(Nm);}
			catch(NumberFormatException e){Nm=handleIntegerException(cargo.getNm(),txt_Nm);}
			catch(NegativeIntegerException e){Nm=handleIntegerException(cargo.getNm(),txt_Nm);}	
			catch(LargeMotorNumberException e){Nm=handleMotorNumberException(cargo.getNm(),txt_Nm);}
		try{FsM=Double.valueOf(txt_FsM.getText()).doubleValue();positiveDoubleCheck(FsM);}
			catch(NumberFormatException e){FsM=handleDoubleException(cargo.getFsM(),txt_FsM);}
			catch(NegativeDoubleException e){FsM=handleDoubleException(cargo.getFsM(),txt_FsM);}
		try{FdM=Double.valueOf(txt_FdM.getText()).doubleValue();positiveDoubleCheck(FdM);}
			catch(NumberFormatException e){FdM=handleDoubleException(cargo.getFdM(),txt_FdM);}
			catch(NegativeDoubleException e){FdM=handleDoubleException(cargo.getFdM(),txt_FdM);}
		try{epsM=Double.valueOf(txt_epsM.getText()).doubleValue();positiveDoubleCheck(epsM);}
			catch(NumberFormatException e){epsM=handleDoubleException(cargo.getepsM(),txt_epsM);}
			catch(NegativeDoubleException e){epsM=handleDoubleException(cargo.getepsM(),txt_epsM);}
		try{piM=Double.valueOf(txt_piM.getText()).doubleValue();positiveDoubleCheck(piM);}
			catch(NumberFormatException e){piM=handleDoubleException(cargo.getpiM(),txt_piM);}
			catch(NegativeDoubleException e){piM=handleDoubleException(cargo.getpiM(),txt_piM);}
		try{vFM=Double.valueOf(txt_vFM.getText()).doubleValue();positiveDoubleCheck(vFM);}
			catch(NumberFormatException e){vFM=handleDoubleException(cargo.getvFM(),txt_vFM);}
			catch(NegativeDoubleException e){vFM=handleDoubleException(cargo.getvFM(),txt_vFM);}
		try{vBM=Double.valueOf(txt_vBM.getText()).doubleValue()/1000.;positiveDoubleCheck(vBM);}
			catch(NumberFormatException e){vBM=handleDoubleException(cargo.getvBM()*1000,txt_vBM);vBM/=1000.;}
			catch(NegativeDoubleException e){vBM=handleDoubleException(cargo.getvBM()*1000,txt_vBM);vBM/=1000.;}
		cargo.setCargoParas(Np,Nm,FsP,FdP,epsP,piP,vFP,vBP,FsM,FdM,epsM,piM,vFM,vBM);
		getCargoParas();
		drawStartState();	
		run=false;
	}
		//displayCargoParas();
}// end actionPerformed

class TimeChoiceListener implements ItemListener {	// choice menu
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getStateChange() == ItemEvent.SELECTED) {
			try {
				timeFactor=Integer.parseInt((String)(ie.getItem()));
				sleepTime=round(cargo.getdt()*1000*timeFactor); // this ensures that the simulation runs in real time:
					if(sleepTime<1){sleepTime=1;}
				txt_message.setForeground(Color.black);
				txt_message.setText(" Time is now slowed down by factor "+timeFactor+". ");
			}
			catch (Exception e) {}
		}
	}// end itemStateChanged
}// end TimeChoiceListener

class RunPauseListener implements ItemListener {	// choice menu
	public void itemStateChanged(ItemEvent ie) {
		if (ie.getStateChange() == ItemEvent.SELECTED) {
			txt_message.setForeground(Color.black);
			txt_message.setText(" Pausing simulation. Click the Run/Pause to continue. ");
		}
		else{
			txt_message.setForeground(Color.black);
			txt_message.setText(" Running simulation. Click the Run/Pause button to pause. "
				+"\n You can change the motor parameters listed below (number + Enter).");
		}
	}// end itemStateChanged
}// end RunPauseListener

//==========================================================================
//==========================================================================
// Exception handling
//==========================================================================
//==========================================================================
class NegativeDoubleException extends Exception{
	public static final long serialVersionUID = 24362462L;
	public NegativeDoubleException(){super();}
	public NegativeDoubleException(String s){super(s);}
}
private final void positiveDoubleCheck(double a) throws NegativeDoubleException{
	if(a<=0){throw new NegativeDoubleException();}
}
private final double handleDoubleException(double aOld,JTextField txt){
	txt_message.setForeground(Color.red);
	txt_message.setText(" Input not valid, must be a positive number. ");
	txt.setText(String.valueOf(aOld));
	//txt_message.setForeground(Color.black);
	return aOld;
}

class NegativeIntegerException extends Exception{
	public static final long serialVersionUID = 24362462L;
	public NegativeIntegerException(){super();}
	public NegativeIntegerException(String s){super(s);}
}
private final void positiveIntegerCheck(int n) throws NegativeIntegerException{
	if(n<0){throw new NegativeIntegerException();}
}
private final int handleIntegerException(int nOld,JTextField txt){
	txt_message.setForeground(Color.red);
	txt_message.setText(" Input not valid, must be a positive integer. ");
	txt.setText(String.valueOf(nOld));
	return nOld;
}

class LargeMotorNumberException extends Exception{
	public static final long serialVersionUID = 24362462L;
	public LargeMotorNumberException(){super();}
	public LargeMotorNumberException(String s){super(s);}
}
private final void motorNumberCheck(int n) throws LargeMotorNumberException{
	if(n>NMax){throw new LargeMotorNumberException();}
}
private final int handleMotorNumberException(int nOld,JTextField txt){
	txt_message.setForeground(Color.red);
	txt_message.setText(" The maximal motor number is "+NMax+". ");
	txt.setText(String.valueOf(nOld));
	//txt_message.setForeground(Color.black);
	return nOld;
}


//=======================================================================
// Applet info
public String getAppletInfo(){return "Tug-of-war animation written by M.J.I. Mueller";}
//=======================================================================
public static final long serialVersionUID = 24362462L;

}//end public class TugOfWarAnimation

