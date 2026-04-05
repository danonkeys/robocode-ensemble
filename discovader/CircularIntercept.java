package discovader;
import java.awt.Point;

/**
 * CircularIntercept
 */
public class CircularIntercept extends Intercept 
{
	protected Point getEstimatedPosition(double time) 
	{
		if (Math.abs(angularVelocity_rad_per_sec) 
			<= Math.toRadians(0.1))
		{
			return super.getEstimatedPosition(time);
		}
		
		double initialTargetHeading = Math.toRadians(targetHeading);
		double finalTargetHeading   = initialTargetHeading +  
		angularVelocity_rad_per_sec * time;
		double x = targetStartingPoint.x - targetVelocity /
		angularVelocity_rad_per_sec *(Math.cos(finalTargetHeading) - 
									  Math.cos(initialTargetHeading));
		double y = targetStartingPoint.y - targetVelocity / 
		angularVelocity_rad_per_sec *
		(Math.sin(initialTargetHeading) - 
		 Math.sin(finalTargetHeading));
		Point p=new Point();
		p.setLocation(x,y);
		return p;
	}	
}