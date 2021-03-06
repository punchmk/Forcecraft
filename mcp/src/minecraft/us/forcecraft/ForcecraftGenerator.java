package us.forcecraft;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import argo.jdom.JsonNode;
import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.Loader;

public class ForcecraftGenerator implements IWorldGenerator {
	private static final int STORY_HEIGHT = 5;
	
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.dimensionId == 7) {
            generateSurface(world, random, chunkX, chunkZ);
        }
	}

	private void generateSurface(World world, Random rand, int chunkX, int chunkZ) {    	
    	generateBuilding(world, rand, chunkX, chunkZ);
	}
	
	
	
	private void generateBuilding(World world, Random rand, int chunkX,
			int chunkZ) {
    	List<JsonNode> records = Forcecraft.instance.accounts.getNode("records").getElements();
		
		if (chunkZ == 0 && chunkX >= 0 && chunkX < records.size()) {
	    	JsonNode acct = records.get(chunkX);
			List<JsonNode> oppys = null;
            int height = 1;
	    	try {
				oppys = acct.getNode("Opportunities", "records").getElements();
	            height = Math.max(1, oppys.size());
    		} catch (IllegalArgumentException iae) {
    			// No data
    		}
            
            for (int l = 0; l < height; l++) {
        		generateLevel(world, chunkX, chunkZ, l, acct, ((oppys != null) && (l < oppys.size())) ? oppys.get(l) : null, Forcecraft.instance.stages);
            }
            generateRoof(world, chunkX, chunkZ, height);
            generateContacts(world, chunkX, chunkZ, height, Forcecraft.instance, records);
		}
	}

	private void generateContacts(World world, int chunkX, int chunkZ, int height, Forcecraft forcecraft, List<JsonNode> records) {
		// x 9, height, z 10
    	try {
			List<JsonNode> contacts = records.get(chunkX).getNode("Contacts", "records").getElements();
			int nContacts = Math.min(contacts.size(), (7 * height * 8));
			for (int i = 0; i < contacts.size(); i++) {
		        JsonNode contact = contacts.get(i);
				String id = contact.getStringValue("Id");
		        EntityContact entityContact = new EntityContact(world, id);
		        entityContact.setLocationAndAngles((double)((chunkX * 16) + ((i / STORY_HEIGHT) % 7) + 6) + 0.5D, 
		        		(double)Forcecraft.groundLevel + ((i % height) * STORY_HEIGHT) + 1.0D, 
		        		(double)((chunkZ * 16) + ((i / (STORY_HEIGHT * 7)) % 8) + 6) + 0.5D, 0.0F, 0.0F);
		        entityContact.setCustomNameTag(contact.getStringValue("Name"));
		        System.out.println("Spawning contact "+entityContact.getCustomNameTag()+" at "+entityContact.getPosition(1.0F));
		        world.spawnEntityInWorld(entityContact);
			}
		} catch (IllegalArgumentException iae) {
			// No data
		}
	}

	private void generateLevel(World world, int chunkX, int chunkZ, int l,
			JsonNode acct, JsonNode oppy, List<JsonNode> stages) {
		// i, j, k are world co-ordinates of lower front right corner of floor
		int i = (chunkX * 16) + 4,
			j = Forcecraft.groundLevel + (l*STORY_HEIGHT),
			k = (chunkZ * 16) + 4,
			p, q, r;
        
		// x, y, x are relative to lower front right corner of floor
    	for (int x = 0; x < 12; x++) {
        	for (int y = 0; y < STORY_HEIGHT; y++) {
	    		for (int z = 0; z < 12; z++) {
        			int metadata = 0; 
        			
        			p = i+x; q = j+y; r = k+z;
	    			
	        		if (x == 0 || x == 11 || z == 0 || z == 11) { // Walls
	        			if ((y == 2 || y == 3) && // Window height
	        					((x == 2 || x == 3 || x == 8 || x == 9) || // side windows 
	        							((l > 0 || z == 11 || y == 3) && (x == 5 || x == 6)))) { // front/back windows
	        				world.setBlock(p, q, r, Block.glass.blockID, 0, 2);
	        			} else if ((l == 0 && z == 0) && (x == 5 || x == 6) && (y == 1 || y == 2)) {
	        				if (y == 2) {
	        					// top half
		        				metadata = 0x8; 
		        				if (x == 5) {
		        					metadata |= 0x1; // hinge on left, else hinge on right
		        				}
	        				} else {
	        					// bottom half
	        					metadata = 0x1; // face north when closed
	        				}
	        				world.setBlock(p, q, r, Block.doorWood.blockID, metadata, 2);
	        			} else if (oppy != null && y==3 && x == 0 && z > 0 && z < 11) { // Stage Block
		    				if (oppy.getStringValue("StageName").equals(stages.get(10-z).getStringValue("label"))) {
		    					metadata = 0x1; // Block is 'on'
		    				}
		    	    		world.setBlock(p, q, r, 500, metadata, 2);
		    	    		TileEntityStageBlock tileentitystageblock = (TileEntityStageBlock)world.getBlockTileEntity(p, q, r);
		    	    		tileentitystageblock.setOpportunityStage(oppy.getStringValue("Id"), 
		    	    				stages.get(10-z).getStringValue("label"));
		    			} else {
		    				world.setBlock(p, q, r, Block.stone.blockID, 0, 2);
		    			}
	    			} else if (y == 0 && (x < 10 || (z < 4 || z > 8))) { // ceiling
	    				int blockID = Block.blockNetherQuartz.blockID;
	    				if ((x == 2 || x == 4 || x == 6 || x == 8) && z > 1 && z < 8) {
	    					blockID = Block.glowStone.blockID; // 'lights'
	    				}
	    				world.setBlock(p, q, r, blockID, 0, 2);
	    			} else if (x == 10 && ((y > 0 && z == (y + 3)) || (l > 0 && y == 0 && z == 8))) { // stairs
	    				world.setBlock(p, q, r, Block.stairsWoodOak.blockID, 0x2, 2);
	    			} else if (y == 1 && (l == 0 || x < 10 || (z < 4 || z > 8))) { // floor
	    				if ((x + z) % 2 == 0) {
	    					metadata = 11; // blue carpet, otherwise white carpet
	    				}
	    				world.setBlock(p, q, r, Block.carpet.blockID, metadata, 2);
	    			} else if (oppy != null && y==2 && x == 1 && z > 0 && z < 11) { // Stage sign
    		    		world.setBlock(p, q, r, Block.signWall.blockID, 5 /* Face east */, 2);
    		    		TileEntitySign tileentitysign = (TileEntitySign)world.getBlockTileEntity(p, q, r);
    		            tileentitysign.signText = splitIntoLines(stages.get(10-z).getStringValue("label"), 16, 4);
	    			} else if (oppy != null && y==3 && x == 1 && z > 0 && z < 11) { // Stage lever
	    				metadata = 0x1; // Face east
	    				if (oppy.getStringValue("StageName").equals(stages.get(10-z).getStringValue("label"))) {
	    					metadata |= 0x8; // Lever is 'on'
	    				}
	    				world.setBlock(p, q, r, Block.lever.blockID, metadata, 2);
	    			}
	    		}
	    	}
		}
    	
    	// Opportunity Name
    	if (oppy != null) {
        	p = i+1; q = j+4; r = k+5;
    		world.setBlock(p, q, r, Block.signWall.blockID, 5 /* Face east */, 2);
    		TileEntitySign tileentitysign = (TileEntitySign)world.getBlockTileEntity(p, q, r);
            tileentitysign.signText = splitIntoLines(oppy.getStringValue("Name"), 16, 4);
    	}
        
    	if (l == 0) {
    		// Account Name
        	p = i+4; q = j+2; r = k-1;
    		world.setBlock(p, q, r, Block.signWall.blockID, 2 /* Face north */, 2);
    		TileEntitySign tileentitysign = (TileEntitySign)world.getBlockTileEntity(p, q, r);
            tileentitysign.signText = splitIntoLines(acct.getStringValue("Name"), 16, 4);
    	}
	}

	private void generateRoof(World world, int chunkX, int chunkZ, int height) {
		// i, j, k are world co-ordinates of lower front right corner of floor
		int i = (chunkX * 16) + 4,
			j = Forcecraft.groundLevel + (height*STORY_HEIGHT),
			k = (chunkZ * 16) + 4;
        
		// x, y, x are relative to lower front right corner of floor
    	for (int x = 0; x < 12; x++) {
        	for (int y = 0; y < 2; y++) {
	    		for (int z = 0; z < 12; z++) {
	    			int p = i+x, q = j+y, r = k+z;
	    			
	        		if (x == 0 || x == 11 || z == 0 || z == 11) { // Walls
	        			world.setBlock(p, q, r, Block.stone.blockID, 0, 2);
	    			} else if (y == 0 && (x < 10 || (z < 4 || z > 8))) { // ceiling
	    				int blockID = Block.blockNetherQuartz.blockID;
	    				if ((x == 3 || x == 8) && z > 1 && z < 10) {
	    					blockID = Block.glowStone.blockID; // 'lights'
	    				}
	    				world.setBlock(p, q, r, blockID, 0, 2);
	    			} else if (y == 0 && z == 8) { // stairs
	    				world.setBlock(p, q, r, Block.stairsWoodOak.blockID, 0x2, 2);
	    			} else if (y == 1 && (x < 10 || (z < 4 || z > 8))) { // floor
	    				world.setBlock(p, q, r, Block.stoneSingleSlab.blockID, 0, 2);
	    			}
	    		}
	    	}
		}
	}

	public String[] splitIntoLines(String input, int maxCharInLine, int maxLines){
	    StringTokenizer tok = new StringTokenizer(input, " ");
	    StringBuilder output = new StringBuilder(input.length());
	    int lineLen = 0;
	    while (tok.hasMoreTokens()) {
	        String word = tok.nextToken();

	        while(word.length() > maxCharInLine){
	            output.append(word.substring(0, maxCharInLine-lineLen) + "\n");
	            word = word.substring(maxCharInLine-lineLen);
	            lineLen = 0;
	        }

	        if (lineLen + word.length() > maxCharInLine) {
	            output.append("\n");
	            lineLen = 0;
	        }
	        output.append(word + " ");

	        lineLen += word.length() + 1;
	    }
	    // output.split();
	    // return output.toString();
	    String[] lines = output.toString().split("\n");
	    
		lines = Arrays.copyOf(lines, maxLines);
		for (int n = 0; n < lines.length; n++) {
			if (lines[n] == null) {
				lines[n] = "";
			}
		}
		
		return lines;
	}
}